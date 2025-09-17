
package app.web.inventory.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.config.SecurityUtil;
import app.web.inventory.dto.*;
import app.web.inventory.model.Products;
import app.web.inventory.service.ProductService;

@RestController
@RequestMapping("/api/spaces/{spaceId}/products")
public class ProductController {

        private final ProductService productService;

        public ProductController(ProductService productService) {
                this.productService = productService;
        }

        /**
         * Create a new product in a specific space
         * POST /api/spaces/{spaceId}/products
         */
        @PostMapping
        public ResponseEntity<?> createProduct(
                        @PathVariable UUID spaceId,
                        @Valid @RequestBody CreateProductRequest request) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                Products product = productService.createProduct(
                                currentUserId,
                                spaceId, // Use path variable instead of request body
                                request.getName(),
                                request.getPrice(),
                                request.getCurrentStock(),
                                request.getMinimumQuantity(),
                                request.getMaximumQuantity());

                return ResponseEntity.status(201).body(Map.of(
                                "success", true,
                                "message", "Product created successfully",
                                "data", Map.of(
                                                "id", product.getId(),
                                                "spaceId", product.getSpace().getId(),
                                                "spaceName", product.getSpace().getName(),
                                                "name", product.getName(),
                                                "price", product.getPrice(),
                                                "currentStock", product.getCurrentStock(),
                                                "minimumQuantity", product.getMinimumQuantity(),
                                                "maximumQuantity", product.getMaximumQuantity(),
                                                "createdAt", product.getCreatedAt())));
        }

        /**
         * Get all products in a specific space
         * GET /api/spaces/{spaceId}/products
         */
        @GetMapping
        public ResponseEntity<?> getProductsBySpace(
                        @PathVariable UUID spaceId,
                        @RequestParam(required = false) String search) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                var products = search != null && !search.trim().isEmpty()
                                ? productService.searchProductsByNameInSpace(currentUserId, spaceId, search)
                                : productService.getProductsDtoBySpace(currentUserId, spaceId);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "data", products));
        }

        /**
         * Get a specific product in a space
         * GET /api/spaces/{spaceId}/products/{productId}
         */
        @GetMapping("/{productId}")
        public ResponseEntity<?> getProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                var productOpt = productService.getProductByIdInSpace(productId, spaceId, currentUserId);

                if (productOpt.isEmpty()) {
                        return ResponseEntity.status(404)
                                        .body(Map.of("success", false, "message", "Product not found in this space"));
                }

                Products product = productOpt.get();
                var productData = new HashMap<String, Object>();
                productData.put("id", product.getId());
                productData.put("spaceId", product.getSpace().getId());
                productData.put("spaceName", product.getSpace().getName());
                productData.put("name", product.getName());
                productData.put("price", product.getPrice());
                productData.put("currentStock", product.getCurrentStock());
                productData.put("minimumQuantity", product.getMinimumQuantity());
                productData.put("maximumQuantity", product.getMaximumQuantity());
                productData.put("isLowStock", productService.isLowStock(product));
                productData.put("createdAt", product.getCreatedAt());
                productData.put("updatedAt", product.getUpdatedAt());

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "data", productData));
        }

        /**
         * Update product details in a space
         * PUT /api/spaces/{spaceId}/products/{productId}
         */
        @PutMapping("/{productId}")
        public ResponseEntity<?> updateProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody UpdateProductRequest request) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                Products updatedProduct = productService.updateProductInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getName(),
                                request.getPrice(),
                                request.getMinimumQuantity(),
                                request.getMaximumQuantity());

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Product updated successfully",
                                "data", Map.of(
                                                "id", updatedProduct.getId(),
                                                "name", updatedProduct.getName(),
                                                "price", updatedProduct.getPrice(),
                                                "minimumQuantity", updatedProduct.getMinimumQuantity(),
                                                "maximumQuantity", updatedProduct.getMaximumQuantity(),
                                                "updatedAt", updatedProduct.getUpdatedAt())));
        }

        /**
         * Add stock to product in a space
         * POST /api/spaces/{spaceId}/products/{productId}/stock/add
         */
        @PostMapping("/{productId}/stock/add")
        public ResponseEntity<?> addStock(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody StockOperationRequest request) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                Products updatedProduct = productService.addStockInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getQuantity());

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Stock added successfully",
                                "data", Map.of(
                                                "id", updatedProduct.getId(),
                                                "currentStock", updatedProduct.getCurrentStock(),
                                                "quantityAdded", request.getQuantity(),
                                                "updatedAt", updatedProduct.getUpdatedAt())));
        }

        /**
         * Remove stock from product in a space
         * POST /api/spaces/{spaceId}/products/{productId}/stock/remove
         */
        @PostMapping("/{productId}/stock/remove")
        public ResponseEntity<?> removeStock(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody StockOperationRequest request) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();

                Products updatedProduct = productService.removeStockInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getQuantity());

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Stock removed successfully",
                                "data", Map.of(
                                                "id", updatedProduct.getId(),
                                                "currentStock", updatedProduct.getCurrentStock(),
                                                "quantityRemoved", request.getQuantity(),
                                                "isLowStock", productService.isLowStock(updatedProduct),
                                                "updatedAt", updatedProduct.getUpdatedAt())));
        }

        /**
         * Delete a product from a space
         * DELETE /api/spaces/{spaceId}/products/{productId}
         */
        @DeleteMapping("/{productId}")
        public ResponseEntity<?> deleteProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();
                productService.deleteProductInSpace(productId, spaceId, currentUserId);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Product deleted successfully"));
        }

        /**
         * Get products with low stock in a specific space
         * GET /api/spaces/{spaceId}/products/low-stock
         */
        @GetMapping("/low-stock")
        public ResponseEntity<?> getLowStockProducts(@PathVariable UUID spaceId) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();
                var products = productService.getLowStockProductsInSpace(currentUserId, spaceId);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Found " + products.size() + " products with low stock",
                                "data", products));
        }
}