package app.web.inventory.controller;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.auth.LoginRequest;
import app.web.inventory.dto.auth.LoginResponseDto;
import app.web.inventory.dto.auth.OtpRequest;
import app.web.inventory.dto.auth.OtpVerifyResponseDto;
import app.web.inventory.dto.auth.RegisterRequest;
import app.web.inventory.dto.user.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.model.Users;
import app.web.inventory.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user or continue an existing unverified registration.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequest request) {
        Users user = authService.register(
                request.getName(), request.getEmail(), request.getPassword());

        UserResponseDto userDto = new UserResponseDto(
                user.getId(), user.getEmail(), user.getName(), user.isVerified(), user.getCreatedAt());

        return ResponseEntity.ok(
                ApiResponse.success("Registration started. OTP sent to email", userDto));
    }

    /**
     * Verify OTP.
     * POST /api/auth/verify-otp
     **/
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<OtpVerifyResponseDto>> verifyOtp(@RequestBody OtpRequest request) {
        int attemptsRemaining = authService.verifyOtp(request.getEmail(), request.getCode());

        OtpVerifyResponseDto dto = new OtpVerifyResponseDto(true, attemptsRemaining, false);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", dto));
    }

    /**
     * Login with email and password.
     * POST /api/auth/login
     **/
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginWithEmailAndPassword(request.getEmail(), request.getPassword());
        LoginResponseDto loginResponse = new LoginResponseDto(token);
        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    /**
     * Resend OTP.
     * POST /api/auth/resend-otp
     **/
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody Map<String, @jakarta.validation.constraints.Email String> body) {

        String email = body.get("email");
        authService.sendOtp(email);

        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", "OTP sent"));
    }
}