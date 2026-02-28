package app.web.inventory.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.pagination.PaginationDto;
import app.web.inventory.dto.product.CreateProductRequest;
import app.web.inventory.dto.product.ProductDto;
import app.web.inventory.dto.product.ProductListDto;
import app.web.inventory.dto.product.ProductResponseDto;
import app.web.inventory.dto.product.UpdateProductRequest;
import app.web.inventory.dto.stock.StockOperationRequest;
import app.web.inventory.service.ProductService;
import app.web.inventory.util.SecurityUtil;
import jakarta.validation.Valid;

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
        public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
                        @PathVariable UUID spaceId,
                        @Valid @RequestBody CreateProductRequest request) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();

                ProductResponseDto product = productService.createProduct(
                                currentUserId,
                                spaceId,
                                request.getName(),
                                request.getPrice(),
                                request.getCurrentStock(),
                                request.getMinimumQuantity(),
                                request.getMaximumQuantity());

                return ResponseEntity.status(201)
                                .body(ApiResponse.success("Product created successfully", product));
        }

        /**
         * Get all products in a specific space
         * GET /api/spaces/{spaceId}/products
         */
        @GetMapping
        public ResponseEntity<ApiResponse<ProductListDto>> getProductsBySpace(
                        @PathVariable UUID spaceId,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "${app.pagination.default-page}") int page,
                        @RequestParam(defaultValue = "${app.pagination.default-size}") int size,
                        @RequestParam(defaultValue = "name") String sortBy,
                        @RequestParam(defaultValue = "ASC") String sortDirection) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();

                Page<ProductDto> productsPage = productService.getProductsBySpace(
                                currentUserId, spaceId, search, page, size, sortBy, sortDirection);

                PaginationDto pagination = new PaginationDto(
                                productsPage.getNumber(),
                                productsPage.getSize(),
                                productsPage.getTotalElements(),
                                productsPage.getTotalPages(),
                                productsPage.hasNext(),
                                productsPage.hasPrevious());

                ProductListDto response = new ProductListDto(productsPage.getContent(), pagination);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Get a specific product in a space
         * GET /api/spaces/{spaceId}/products/{productId}
         */
        @GetMapping("/{productId}")
        public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();
                ProductResponseDto product = productService.getProductByIdInSpace(productId, spaceId, currentUserId);

                return ResponseEntity.ok(ApiResponse.success(product));
        }

        /**
         * Update product details in a space
         * PUT /api/spaces/{spaceId}/products/{productId}
         */
        @PutMapping("/{productId}")
        public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody UpdateProductRequest request) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();

                ProductResponseDto updatedProduct = productService.updateProductInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getName(),
                                request.getPrice(),
                                request.getMinimumQuantity(),
                                request.getMaximumQuantity());

                return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
        }

        /**
         * Add stock to product in a space
         * POST /api/spaces/{spaceId}/products/{productId}/stock/add
         */
        @PostMapping("/{productId}/stock/add")
        public ResponseEntity<ApiResponse<ProductResponseDto>> addStock(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody StockOperationRequest request) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();

                ProductResponseDto updatedProduct = productService.addStockInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getQuantity());

                return ResponseEntity.ok(ApiResponse.success("Stock added successfully", updatedProduct));
        }

        /**
         * Remove stock from product in a space
         * POST /api/spaces/{spaceId}/products/{productId}/stock/remove
         */
        @PostMapping("/{productId}/stock/remove")
        public ResponseEntity<ApiResponse<ProductResponseDto>> removeStock(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId,
                        @Valid @RequestBody StockOperationRequest request) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();

                ProductResponseDto updatedProduct = productService.removeStockInSpace(
                                productId,
                                spaceId,
                                currentUserId,
                                request.getQuantity());

                return ResponseEntity.ok(ApiResponse.success("Stock removed successfully", updatedProduct));
        }

        /**
         * Delete a product from a space
         * DELETE /api/spaces/{spaceId}/products/{productId}
         */
        @DeleteMapping("/{productId}")
        public ResponseEntity<ApiResponse<Void>> deleteProduct(
                        @PathVariable UUID spaceId,
                        @PathVariable UUID productId) {

                UUID currentUserId = SecurityUtil.getCurrentUserId();
                productService.deleteProductInSpace(productId, spaceId, currentUserId);

                return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        }

        /**
         * Get products with low stock in a specific space
         * GET /api/spaces/{spaceId}/products/low-stock
         */
        @GetMapping("/low-stock")
        public ResponseEntity<ApiResponse<List<ProductDto>>> getLowStockProducts(@PathVariable UUID spaceId) {
                UUID currentUserId = SecurityUtil.getCurrentUserId();
                List<ProductDto> products = productService.getLowStockProductsInSpace(currentUserId, spaceId);

                String message = !products.isEmpty()
                                ? "Found " + products.size() + " products with low stock"
                                : "No low stock products found";

                return ResponseEntity.ok(ApiResponse.success(message, products));
        }
}