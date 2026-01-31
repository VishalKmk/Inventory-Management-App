package app.web.inventory.controller;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.auth.LoginRequest;
import app.web.inventory.dto.auth.LoginResponseDto;
import app.web.inventory.dto.auth.OtpRequest;
import app.web.inventory.dto.auth.RegisterRequest;
import app.web.inventory.dto.user.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.model.Users;
import app.web.inventory.service.AuthService;
import app.web.inventory.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequest request) {
        Users user = userService.register(request.getName(), request.getEmail(), request.getPassword());
        authService.sendOtp(request.getEmail());

        UserResponseDto userDto = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.getCreatedAt());

        return ResponseEntity.status(201)
                .body(ApiResponse.success("User registered. OTP sent to email", userDto));
    }

    /**
     * Verify OTP
     * POST /api/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody OtpRequest request) {
        boolean isValid = authService.verifyOtp(request.getEmail(), request.getCode());

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired OTP"));
        }

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", "OTP verified"));
    }

    /**
     * Login with email and password
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginWithEmailAndPassword(request.getEmail(), request.getPassword());

        LoginResponseDto loginResponse = new LoginResponseDto(token);

        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    /**
     * Resend OTP
     * POST /api/auth/resend-otp
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody Map<String, @jakarta.validation.constraints.Email String> body) {

        String email = body.get("email");
        authService.sendOtp(email);

        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", "OTP sent"));
    }
}