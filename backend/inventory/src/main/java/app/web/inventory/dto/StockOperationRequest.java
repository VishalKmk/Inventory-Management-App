package app.web.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockOperationRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;
}