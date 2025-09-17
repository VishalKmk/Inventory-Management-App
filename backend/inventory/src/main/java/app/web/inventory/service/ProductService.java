package app.web.inventory.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import app.web.inventory.dto.ProductDto;
import app.web.inventory.model.Products;
import app.web.inventory.model.Spaces;
import app.web.inventory.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;

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
    // HIERARCHICAL METHODS (Space -> Product operations)
    // =============================================================================

    /**
     * Create a new product in a specific space (hierarchical)
     */
    public Products createProduct(UUID ownerId, UUID spaceId, String name, Double price,
            Integer currentStock, Integer minimumQuantity, Integer maximumQuantity) {

        // Verify user owns the space
        Spaces space = spaceService.getSpaceByIdAndOwner(spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found or access denied"));

        // Validate input
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
                ownerId,
                "PRODUCT",
                savedProduct.getId(),
                "CREATE",
                details,
                getClientIpAddress(),
                getUserAgent(),
                spaceId,
                "SPACE");

        return productRepository.save(product);
    }

    /**
     * Get a specific product by ID within a specific space (hierarchical)
     */
    public Optional<Products> getProductByIdInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        // First verify space ownership
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new IllegalArgumentException("Space not found or access denied");
        }

        // Find product and verify it belongs to the specified space
        Optional<Products> productOpt = productRepository.findByIdAndOwnerId(productId, ownerId);

        if (productOpt.isPresent() && !productOpt.get().getSpace().getId().equals(spaceId)) {
            return Optional.empty(); // Product exists but not in the specified space
        }

        return productOpt;
    }

    /**
     * Update product details in a specific space (hierarchical)
     */
    public Products updateProductInSpace(UUID productId, UUID spaceId, UUID ownerId,
            String name, Double price, Integer minimumQuantity, Integer maximumQuantity) {

        Products product = getProductByIdInSpace(productId, spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in this space or access denied"));

        // Track changes
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

        Products updatedProduct = productRepository.save(product);

        // Log the update if there were changes
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

        return updatedProduct;
    }

    /**
     * Adds stock to a product in a specific space.
     *
     * @param productId The unique identifier of the product to update
     * @param spaceId   The unique identifier of the space where the product is
     *                  located
     * @param ownerId   The unique identifier of the user performing the operation
     * @param quantity  The amount of stock to add (must be positive)
     * @return The updated Products entity with the new stock quantity
     * @throws IllegalArgumentException if the product is not found in the space, if
     *                                  access is denied,
     *                                  or if the quantity is null or not positive
     */
    public Products addStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Products product = getProductByIdInSpace(productId, spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in this space or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        Integer oldStock = product.getCurrentStock();
        Integer newStock = oldStock + quantity;
        product.setCurrentStock(newStock);
        Products updatedProduct = productRepository.save(product);

        // Log the stock addition
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

        return updatedProduct;
    }

    /**
     * Removes a specified quantity of stock from a product in a specific space.
     *
     * @param productId The UUID of the product to update
     * @param spaceId   The UUID of the space where the product is located
     * @param ownerId   The UUID of the user performing the operation
     * @param quantity  The amount of stock to remove (must be positive)
     * @return The updated Products entity with the new stock level
     * @throws IllegalArgumentException if:
     *                                  - The product is not found in the specified
     *                                  space
     *                                  - The user doesn't have access to the space
     *                                  - The quantity is null or non-positive
     *                                  - The requested quantity exceeds current
     *                                  stock
     */
    public Products removeStockInSpace(UUID productId, UUID spaceId, UUID ownerId, Integer quantity) {
        Products product = getProductByIdInSpace(productId, spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in this space or access denied"));

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

        // Log the stock removal
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

        return updatedProduct;
    }

    /**
     * Delete a product from a specific space (hierarchical)
     */
    public boolean deleteProductInSpace(UUID productId, UUID spaceId, UUID ownerId) {
        Products product = getProductByIdInSpace(productId, spaceId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in this space or access denied"));

        String productName = product.getName();
        String spaceName = product.getSpace().getName();
        Double productValue = product.getPrice() * product.getCurrentStock();

        productRepository.delete(product);

        // Log the deletion
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

        return true;
    }

    /**
     * Search products by name within a specific space (hierarchical)
     */
    public List<ProductDto> searchProductsByNameInSpace(UUID ownerId, UUID spaceId, String name) {
        // Verify space ownership
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new IllegalArgumentException("Space not found or access denied");
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
        // Verify space ownership
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new IllegalArgumentException("Space not found or access denied");
        }

        return productRepository.findLowStockProductsBySpaceId(spaceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // GLOBAL METHODS (All products across all spaces)
    // =============================================================================

    /**
     * Get all products owned by a user (across all their spaces)
     */
    public List<Products> getProductsByOwner(UUID ownerId) {
        return productRepository.findByOwnerId(ownerId);
    }

    /**
     * Get all products owned by a user as DTOs (across all spaces)
     */
    public List<ProductDto> getProductsDtoByOwner(UUID ownerId) {
        return productRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all products in a specific space as DTOs
     */
    public List<ProductDto> getProductsDtoBySpace(UUID ownerId, UUID spaceId) {
        return getProductsBySpace(ownerId, spaceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all products in a specific space
     */
    public List<Products> getProductsBySpace(UUID ownerId, UUID spaceId) {
        // Verify user owns the space
        if (!spaceService.hasAccessToSpace(spaceId, ownerId)) {
            throw new IllegalArgumentException("Space not found or access denied");
        }

        return productRepository.findBySpaceId(spaceId);
    }

    /**
     * Get a product by ID, ensuring it belongs to the owner (global lookup)
     */
    public Optional<Products> getProductByIdAndOwner(UUID productId, UUID ownerId) {
        return productRepository.findByIdAndOwnerId(productId, ownerId);
    }

    /**
     * Search products by name across all spaces
     */
    public List<Products> searchProductsByName(UUID ownerId, String name) {
        if (name == null || name.trim().isEmpty()) {
            return getProductsByOwner(ownerId);
        }
        return productRepository.findByOwnerIdAndNameContainingIgnoreCase(ownerId, name.trim());
    }

    /**
     * Search products by name as DTOs across all spaces
     */
    public List<ProductDto> searchProductsByNameDto(UUID ownerId, String name) {
        return searchProductsByName(ownerId, name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get products with low stock across all spaces
     */
    public List<Products> getLowStockProducts(UUID ownerId) {
        return productRepository.findLowStockProductsByOwnerId(ownerId);
    }

    /**
     * Get products with low stock as DTOs across all spaces
     */
    public List<ProductDto> getLowStockProductsDto(UUID ownerId) {
        return getLowStockProducts(ownerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // LEGACY METHODS (for backward compatibility - can be removed later)
    // =============================================================================

    /**
     * @deprecated Use updateProductInSpace instead
     */
    @Deprecated
    public Products updateProduct(UUID productId, UUID ownerId, String name, Double price,
            Integer minimumQuantity, Integer maximumQuantity) {

        Products product = getProductByIdAndOwner(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or access denied"));

        if (name != null && !name.trim().isEmpty()) {
            product.setName(name.trim());
        }
        if (price != null && price >= 0) {
            product.setPrice(price);
        }
        if (minimumQuantity != null && minimumQuantity >= 0) {
            product.setMinimumQuantity(minimumQuantity);
        }
        if (maximumQuantity != null && maximumQuantity >= 0) {
            product.setMaximumQuantity(maximumQuantity);
        }

        return productRepository.save(product);
    }

    /**
     * @deprecated Use updateStockInSpace instead
     */
    @Deprecated
    public Products updateStock(UUID productId, UUID ownerId, Integer newStock) {
        Products product = getProductByIdAndOwner(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or access denied"));

        if (newStock == null || newStock < 0) {
            throw new IllegalArgumentException("Stock must be non-negative");
        }

        product.setCurrentStock(newStock);
        return productRepository.save(product);
    }

    /**
     * @deprecated Use addStockInSpace instead
     */
    @Deprecated
    public Products addStock(UUID productId, UUID ownerId, Integer quantity) {
        Products product = getProductByIdAndOwner(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        product.setCurrentStock(product.getCurrentStock() + quantity);
        return productRepository.save(product);
    }

    /**
     * @deprecated Use removeStockInSpace instead
     */
    @Deprecated
    public Products removeStock(UUID productId, UUID ownerId, Integer quantity) {
        Products product = getProductByIdAndOwner(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or access denied"));

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }

        int newStock = product.getCurrentStock() - quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Current: " + product.getCurrentStock() + ", Requested: " + quantity);
        }

        product.setCurrentStock(newStock);
        return productRepository.save(product);
    }

    /**
     * @deprecated Use deleteProductInSpace instead
     */
    @Deprecated
    public boolean deleteProduct(UUID productId, UUID ownerId) {
        Products product = getProductByIdAndOwner(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or access denied"));

        productRepository.delete(product);
        return true;
    }

    // =============================================================================
    // UTILITY METHODS
    // =============================================================================

    /**
     * Check if product stock is low
     */
    public boolean isLowStock(Products product) {
        return product.getMinimumQuantity() != null &&
                product.getCurrentStock() != null &&
                product.getCurrentStock() <= product.getMinimumQuantity();
    }

    /**
     * Convert Product entity to DTO
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