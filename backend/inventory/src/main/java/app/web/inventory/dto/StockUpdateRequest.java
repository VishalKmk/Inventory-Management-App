package app.web.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @NotNull(message = "Current stock is required")
    @Min(value = 0, message = "Current stock must be non-negative")
    private Integer currentStock;
}