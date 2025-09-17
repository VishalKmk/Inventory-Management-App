package app.web.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProductRequest {

    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    private String name;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private Double price;

    @Min(value = 0, message = "Minimum quantity must be non-negative")
    private Integer minimumQuantity;

    @Min(value = 0, message = "Maximum quantity must be non-negative")
    private Integer maximumQuantity;

    @AssertTrue(message = "Maximum quantity must be greater than or equal to minimum quantity")
    public boolean isMaxQuantityValid() {
        if (minimumQuantity == null || maximumQuantity == null) {
            return true;
        }
        return maximumQuantity >= minimumQuantity;
    }
}
