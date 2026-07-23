package app.web.inventory.dto.space;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateSpaceRequest {
    @NotBlank(message = "Space name is required")
    @Size(min = 1, max = 255, message = "Space name must be between 1 and 255 characters")
    private String name;
}
