package app.web.inventory.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductRequest {
    // Remove spaceId - it comes from the URL path now

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    private String name;

    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String imageUrl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private Double price;

    @NotNull(message = "Current stock is required")
    @Min(value = 0, message = "Current stock must be non-negative")
    private Integer currentStock;

    @Min(value = 0, message = "Minimum quantity must be non-negative")
    private Integer minimumQuantity;

    @Min(value = 0, message = "Maximum quantity must be non-negative")
    private Integer maximumQuantity;

    // Custom validation to ensure max >= min
    @AssertTrue(message = "Maximum quantity must be greater than or equal to minimum quantity")
    public boolean isMaxQuantityValid() {
        if (minimumQuantity == null || maximumQuantity == null) {
            return true; // Skip validation if either is null
        }
        return maximumQuantity >= minimumQuantity;
    }
}
