package app.web.inventory.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.web.inventory.dto.product.ProductDto;
import app.web.inventory.dto.product.ProductResponseDto;
import app.web.inventory.exception.ResourceNotFoundException;
import app.web.inventory.model.Products;
import app.web.inventory.model.Spaces;
import app.web.inventory.model.enums.SpaceRole;
import app.web.inventory.repository.ProductRepository;
import app.web.inventory.repository.SpaceMemberRepository;
import app.web.inventory.util.RequestUtil;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final SpaceService spaceService;
    private final AuditLogService auditLogService;
    private final SpaceMemberRepository spaceMemberRepository;

    public ProductService(ProductRepository productRepository, SpaceService spaceService,
            AuditLogService auditLogService, SpaceMemberRepository spaceMemberRepository) {
        this.productRepository = productRepository;
        this.spaceService = spaceService;
        this.auditLogService = auditLogService;
        this.spaceMemberRepository = spaceMemberRepository;
    }

    /**
     * Create a new product in a specific space.
     */
    public ProductResponseDto createProduct(UUID userId, UUID spaceId, String name, String sku, String category,
            String imageUrl, Double price,
            Integer currentStock, Integer minimumQuantity, Integer maximumQuantity) {

        checkWriteAccess(spaceId, userId);

        Spaces space = spaceService.getSpaceById(spaceId);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Product price must be non-negative");
        }
        if (currentStock == null || currentStock < 0) {
            throw new IllegalArgumentException("Current stock must be non-negative");
        }

        Products product = new Products();
        product.setSpace(space);
        product.setName(name.trim());
        product.setSku(normalizeOptional(sku));
        product.setCategory(normalizeOptional(category));
        product.setImageUrl(normalizeOptional(imageUrl));
        product.setPrice(price);
        product.setCurrentStock(currentStock);
        product.setMinimumQuantity(minimumQuantity);
        product.setMaximumQuantity(maximumQuantity);

        Products savedProduct = productRepository.save(product);

        // Log the creation
        Map<String, Object> details = Map.of(
                "productName", savedProduct.getName(),
                "spaceName", space.getName(),
                "price", price,
                "initialStock", currentStock,
                "minimumQuantity", minimumQuantity != null ? minimumQuantity : 0,
                "maximumQuantity", maximumQuantity != null ? maximumQuantity : 0,
                "action", "Product created");
        auditLogService.logAction(
                userId,
                "PRODUCT",
                savedProduct.getId(),
                "CREATE",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(savedProduct);
    }

    // Get all accessible products for a user (owned + member spaces)
    public List<Products> getAccessibleProducts(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Get from owned spaces
        List<Products> ownedProducts = productRepository.findByOwnerId(userId);

        // Get from member spaces
        List<Products> memberProducts = spaceMemberRepository
                .findActiveSpacesByUserId(userId)
                .stream()
                .filter(sm -> !sm.getSpace().getOwner().getId().equals(userId))
                .flatMap(sm -> productRepository.findBySpaceId(sm.getSpace().getId()).stream())
                .collect(Collectors.toList());

        List<Products> all = new ArrayList<>(ownedProducts);
        all.addAll(memberProducts);
        return all;
    }

    public List<Products> getAccessibleLowStockProducts(UUID userId) {
        return getAccessibleProducts(userId).stream()
                .filter(this::isLowStock)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific product by ID within a specific space.
     */
    public ProductResponseDto getProductByIdInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        checkReadAccess(spaceId, ownerId);

        Products product = productRepository.findByIdAndUserHasAccess(productId, ownerId) // ← was userId
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space"));

        return convertToResponseDto(product);
    }

    // Update product details in a specific space.
    public ProductResponseDto updateProductInSpace(UUID productId, UUID spaceId, UUID ownerId,
            String name, String sku, String category, String imageUrl,
            Double price, Integer minimumQuantity, Integer maximumQuantity) {

        checkWriteAccess(spaceId, ownerId);

        Products product = productRepository.findByIdAndUserHasAccess(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        Map<String, Object> changes = new HashMap<>();
        if (name != null && !name.trim().isEmpty() && !name.trim().equals(product.getName())) {
            changes.put("oldName", product.getName());
            changes.put("newName", name.trim());
            product.setName(name.trim());
        }
        if (sku != null && !Objects.equals(normalizeOptional(sku), product.getSku())) {
            changes.put("oldSku", product.getSku());
            changes.put("newSku", normalizeOptional(sku));
            product.setSku(normalizeOptional(sku));
        }
        if (category != null && !Objects.equals(normalizeOptional(category), product.getCategory())) {
            changes.put("oldCategory", product.getCategory());
            changes.put("newCategory", normalizeOptional(category));
            product.setCategory(normalizeOptional(category));
        }
        if (imageUrl != null && !Objects.equals(normalizeOptional(imageUrl), product.getImageUrl())) {
            changes.put("oldImageUrl", product.getImageUrl());
            changes.put("newImageUrl", normalizeOptional(imageUrl));
            product.setImageUrl(normalizeOptional(imageUrl));
        }
        if (price != null && price >= 0 && !price.equals(product.getPrice())) {
            changes.put("oldPrice", product.getPrice());
            changes.put("newPrice", price);
            product.setPrice(price);
        }
        if (minimumQuantity != null && minimumQuantity >= 0 && !minimumQuantity.equals(product.getMinimumQuantity())) {
            changes.put("oldMinimumQuantity", product.getMinimumQuantity());
            changes.put("newMinimumQuantity", minimumQuantity);
            product.setMinimumQuantity(minimumQuantity);
        }
        if (maximumQuantity != null && maximumQuantity >= 0 && !maximumQuantity.equals(product.getMaximumQuantity())) {
            changes.put("oldMaximumQuantity", product.getMaximumQuantity());
            changes.put("newMaximumQuantity", maximumQuantity);
            product.setMaximumQuantity(maximumQuantity);
        }

        @SuppressWarnings("null")
        Products updatedProduct = productRepository.save(product);

        if (!changes.isEmpty()) {
            changes.put("productName", product.getName());
            changes.put("spaceName", product.getSpace().getName());
            changes.put("action", "Product details updated");

            auditLogService.logAction(
                    ownerId,
                    "PRODUCT",
                    productId,
                    "UPDATE",
                    changes,
                    RequestUtil.getClientIpAddress(),
                    RequestUtil.getUserAgent(),
                    spaceId,
                    "SPACE");
        }

        return convertToResponseDto(updatedProduct);
    }

    // Add stock to a product in a specific space.
    public ProductResponseDto addStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        Objects.requireNonNull(spaceId, "Space ID cannot be null");
        Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        checkWriteAccess(spaceId, ownerId);

        Products product = productRepository.findByIdAndUserHasAccess(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        Integer oldStock = product.getCurrentStock();

        int rowsUpdated = productRepository.incrementStock(productId, quantity);

        if (rowsUpdated == 0) {
            // Re-fetch to report the up-to-date state in the error message.
            Products current = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            throw new IllegalArgumentException(
                    "Cannot exceed maximum quantity. Current: " + current.getCurrentStock() +
                            ", Requested: " + quantity + ", Maximum: " + current.getMaximumQuantity());
        }

        Products updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Integer newStock = updatedProduct.getCurrentStock();

        Map<String, Object> details = Map.of(
                "productName", product.getName(),
                "spaceName", product.getSpace().getName(),
                "oldStock", oldStock,
                "newStock", newStock,
                "quantityAdded", quantity,
                "action", "Stock added");
        auditLogService.logAction(
                ownerId,
                "PRODUCT",
                productId,
                "STOCK_ADD",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(updatedProduct);
    }

    /**
     * Remove stock from a product in a specific space.
     */
    public ProductResponseDto removeStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        Objects.requireNonNull(spaceId, "Space ID cannot be null");
        Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        checkWriteAccess(spaceId, ownerId);

        Products product = productRepository.findByIdAndUserHasAccess(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }

        Integer oldStock = product.getCurrentStock();

        // Atomic decrement at the DB level
        int rowsUpdated = productRepository.decrementStock(productId, quantity);

        if (rowsUpdated == 0) {
            Products current = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            throw new IllegalArgumentException(
                    "Insufficient stock. Current: " + current.getCurrentStock() + ", Requested: " + quantity);
        }

        Products updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Integer newStock = updatedProduct.getCurrentStock();

        Map<String, Object> details = Map.of(
                "productName", product.getName(),
                "spaceName", product.getSpace().getName(),
                "oldStock", oldStock,
                "newStock", newStock,
                "quantityRemoved", quantity,
                "action", "Stock removed");
        auditLogService.logAction(
                ownerId,
                "PRODUCT",
                productId,
                "STOCK_REMOVE",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(updatedProduct);
    }

    /**
     * Delete a product from a specific space.
     */
    public void deleteProductInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        SpaceRole role = spaceService.getUserRoleInSpace(spaceId, ownerId); // ← was userId
        if (role == SpaceRole.MEMBER || role == SpaceRole.VIEWER || role == SpaceRole.PENDING) {
            throw new SecurityException("Only owners and admins can delete products");
        }

        Products product = productRepository.findByIdAndUserHasAccess(productId, ownerId) // ← was userId
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        String productName = product.getName();
        String spaceName = product.getSpace().getName();
        Double productValue = product.getPrice() * product.getCurrentStock();

        productRepository.delete(product);

        Map<String, Object> details = Map.of(
                "productName", productName,
                "spaceName", spaceName,
                "finalStock", product.getCurrentStock(),
                "productValue", productValue,
                "action", "Product deleted");
        auditLogService.logAction(
                ownerId,
                "PRODUCT",
                productId,
                "DELETE",
                details,
                RequestUtil.getClientIpAddress(),
                RequestUtil.getUserAgent(),
                spaceId,
                "SPACE");
    }

    /**
     * Search products by name within a specific space.
     */
    public List<ProductDto> searchProductsByNameInSpace(UUID ownerId, UUID spaceId, String name) {
        checkReadAccess(spaceId, ownerId);

        List<Products> products;
        if (name == null || name.trim().isEmpty()) {
            products = productRepository.findBySpaceId(spaceId);
        } else {
            products = productRepository.findBySpaceIdAndNameContainingIgnoreCase(spaceId, name.trim());
        }

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get products with low stock in a specific space.
     */
    public List<ProductDto> getLowStockProductsInSpace(UUID ownerId, UUID spaceId) {
        checkReadAccess(spaceId, ownerId);

        return productRepository.findLowStockProductsBySpaceId(spaceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get products by space with pagination.
     */
    public Page<ProductDto> getProductsBySpace(UUID userId, UUID spaceId, String search,
            int page, int size, String sortBy, String sortDirection) {

        if (!spaceService.hasAccessToSpace(spaceId, userId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

        // Fix: provide safe defaults before passing to Sort.by()
        String safeSort = (sortBy != null && !sortBy.isBlank()) ? sortBy : "name";
        String safeDirection = (sortDirection != null && !sortDirection.isBlank()) ? sortDirection : "ASC";

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(safeDirection), safeSort));

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        return productRepository
                .findBySpaceIdAndUserHasAccessAndSearch(spaceId, userId, searchParam, pageable)
                .map(this::convertToDto);
    }

    public List<Products> getProductsBySpace(UUID ownerId, UUID spaceId) {
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }
        return productRepository.findBySpaceId(spaceId);
    }

    public List<Products> getProductsByOwner(UUID ownerId) {
        return productRepository.findByOwnerId(ownerId);
    }

    public List<Products> getLowStockProducts(UUID ownerId) {
        return productRepository.findLowStockProductsByOwnerId(ownerId);
    }

    private void checkWriteAccess(UUID spaceId, UUID userId) {
        SpaceRole role = spaceService.getUserRoleInSpace(spaceId, userId);
        // VIEWER can't write, PENDING can't do anything
        if (role == SpaceRole.VIEWER || role == SpaceRole.PENDING) {
            throw new SecurityException("Insufficient permissions to modify products");
        }
    }

    private void checkReadAccess(UUID spaceId, UUID userId) {
        if (!spaceService.hasAccessToSpace(spaceId, userId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }
    }

    public boolean isLowStock(Products product) {
        return product.getMinimumQuantity() != null &&
                product.getCurrentStock() != null &&
                product.getCurrentStock() <= product.getMinimumQuantity();
    }

    private String normalizeOptional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Convert Product entity to simple DTO (for lists)
     */
    private ProductDto convertToDto(Products product) {
        return new ProductDto(
                product.getId(),
                product.getSpace().getId(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCurrentStock(),
                product.getMinimumQuantity(),
                product.getMaximumQuantity());
    }

    /**
     * Convert Product entity to full Response DTO (for single items)
     */
    private ProductResponseDto convertToResponseDto(Products product) {
        return new ProductResponseDto(
                product.getId(),
                product.getSpace().getId(),
                product.getSpace().getName(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCurrentStock(),
                product.getMinimumQuantity(),
                product.getMaximumQuantity(),
                isLowStock(product),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}