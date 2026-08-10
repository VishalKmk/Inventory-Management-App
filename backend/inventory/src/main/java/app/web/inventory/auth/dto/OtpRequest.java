package app.web.inventory.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String code;
}