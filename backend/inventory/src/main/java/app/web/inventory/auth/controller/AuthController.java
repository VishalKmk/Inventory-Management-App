package app.web.inventory.auth.controller;

import app.web.inventory.shared.dto.ApiResponse;
import app.web.inventory.auth.dto.LoginRequest;
import app.web.inventory.auth.dto.LoginResponseDto;
import app.web.inventory.auth.dto.OtpRequest;
import app.web.inventory.auth.dto.OtpVerifyResponseDto;
import app.web.inventory.auth.dto.RefreshRequest;
import app.web.inventory.auth.dto.RegisterRequest;
import app.web.inventory.user.dto.UserResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.user.model.Users;
import app.web.inventory.auth.service.AuthService;
import app.web.inventory.shared.util.RequestUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
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
        String clientIp = RequestUtil.getClientIpAddress();
        AuthService.LoginResult result =
                authService.loginWithEmailAndPassword(request.getEmail(), request.getPassword(), clientIp);
        LoginResponseDto loginResponse = new LoginResponseDto(result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    /**
     * Exchange a refresh token for a new access token + rotated refresh token.
     * POST /api/auth/refresh
     **/
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthService.LoginResult result = authService.refresh(request.getRefreshToken());
        LoginResponseDto loginResponse = new LoginResponseDto(result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", loginResponse));
    }

    /**
     * Revoke a refresh token. The access token stays valid until it naturally
     * expires (it's stateless) — this only stops it from being silently renewed.
     * POST /api/auth/logout
     **/
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out", null));
    }

    /**
     * Resend OTP.
     * POST /api/auth/resend-otp
     **/
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody Map<String, @Email String> body) {

        String email = body.get("email");
        authService.resendOtp(email);

        return ResponseEntity.ok(ApiResponse.success(
                "If this account exists and isn't verified, an OTP has been sent.",
                "OTP request accepted"));
    }
}