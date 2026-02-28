package app.web.inventory.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import app.web.inventory.dto.product.ProductDto;
import app.web.inventory.dto.product.ProductResponseDto;
import app.web.inventory.exception.ResourceNotFoundException;
import app.web.inventory.model.Products;
import app.web.inventory.model.Spaces;
import app.web.inventory.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final SpaceService spaceService;
    private final AuditLogService auditLogService;

    public ProductService(ProductRepository productRepository, SpaceService spaceService,
            AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.spaceService = spaceService;
        this.auditLogService = auditLogService;
    }

    // =============================================================================
    // HIERARCHICAL METHODS (Space -> Product operations) - Return DTOs
    // =============================================================================

    /**
     * Create a new product in a specific space (hierarchical)
     */
    public ProductResponseDto createProduct(UUID userId, UUID spaceId, String name, Double price,
            Integer currentStock, Integer minimumQuantity, Integer maximumQuantity) {

        Spaces space = spaceService.getSpaceByIdAndUser(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found or access denied"));

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
                getClientIpAddress(),
                getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(savedProduct);
    }

    /**
     * Get a specific product by ID within a specific space (hierarchical)
     */
    public ProductResponseDto getProductByIdInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

        Products product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space"));

        return convertToResponseDto(product);
    }

    /**
     * Update product details in a specific space (hierarchical)
     */
    public ProductResponseDto updateProductInSpace(UUID productId, UUID spaceId, UUID ownerId,
            String name, Double price, Integer minimumQuantity, Integer maximumQuantity) {

        Products product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        Map<String, Object> changes = new HashMap<>();
        if (name != null && !name.trim().isEmpty() && !name.trim().equals(product.getName())) {
            changes.put("oldName", product.getName());
            changes.put("newName", name.trim());
            product.setName(name.trim());
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
                    getClientIpAddress(),
                    getUserAgent(),
                    spaceId,
                    "SPACE");
        }

        return convertToResponseDto(updatedProduct);
    }

    /**
     * Add stock to a product in a specific space
     */
    public ProductResponseDto addStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Products product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        Integer oldStock = product.getCurrentStock();
        Integer newStock = oldStock + quantity;
        product.setCurrentStock(newStock);
        Products updatedProduct = productRepository.save(product);

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
                getClientIpAddress(),
                getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(updatedProduct);
    }

    /**
     * Remove stock from a product in a specific space
     */
    public ProductResponseDto removeStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Products product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .filter(p -> p.getSpace().getId().equals(spaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in this space or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }

        Integer oldStock = product.getCurrentStock();
        int newStock = oldStock - quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Current: " + oldStock + ", Requested: " + quantity);
        }

        product.setCurrentStock(newStock);
        Products updatedProduct = productRepository.save(product);

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
                getClientIpAddress(),
                getUserAgent(),
                spaceId,
                "SPACE");

        return convertToResponseDto(updatedProduct);
    }

    /**
     * Delete a product from a specific space (hierarchical)
     */
    public void deleteProductInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        Products product = productRepository.findByIdAndOwnerId(productId, ownerId)
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
                getClientIpAddress(),
                getUserAgent(),
                spaceId,
                "SPACE");
    }

    /**
     * Search products by name within a specific space (hierarchical)
     */
    public List<ProductDto> searchProductsByNameInSpace(UUID ownerId, UUID spaceId, String name) {
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

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
     * Get products with low stock in a specific space (hierarchical)
     */
    public List<ProductDto> getLowStockProductsInSpace(UUID ownerId, UUID spaceId) {
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

        return productRepository.findLowStockProductsBySpaceId(spaceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // GLOBAL METHODS - Return DTOs
    // =============================================================================

    /**
     * Get products by space with pagination
     */
    public Page<ProductDto> getProductsBySpace(UUID ownerId, UUID spaceId, String search, int page, int size,
            String sortBy, String sortDirection) {
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new ResourceNotFoundException("Space not found or access denied");
        }

        String direction = sortDirection != null ? sortDirection : "ASC";
        String property = sortBy != null ? sortBy : "name";
        Sort sort = Sort.by(Sort.Direction.fromString(direction), property);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Products> productsPage;
        if (search != null && !search.trim().isEmpty()) {
            productsPage = productRepository.findBySpaceIdAndNameContainingIgnoreCase(spaceId, search.trim(), pageable);
        } else {
            productsPage = productRepository.findBySpaceId(spaceId, pageable);
        }

        return productsPage.map(this::convertToDto);
    }

    public List<ProductDto> getProductsDtoBySpace(UUID ownerId, UUID spaceId) {
        return getProductsBySpace(ownerId, spaceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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

    // =============================================================================
    // UTILITY METHODS
    // =============================================================================

    public boolean isLowStock(Products product) {
        return product.getMinimumQuantity() != null &&
                product.getCurrentStock() != null &&
                product.getCurrentStock() <= product.getMinimumQuantity();
    }

    /**
     * Convert Product entity to simple DTO (for lists)
     */
    private ProductDto convertToDto(Products product) {
        return new ProductDto(
                product.getId(),
                product.getSpace().getId(),
                product.getName(),
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
                product.getPrice(),
                product.getCurrentStock(),
                product.getMinimumQuantity(),
                product.getMaximumQuantity(),
                isLowStock(product),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    // Helper methods for request context
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }
}