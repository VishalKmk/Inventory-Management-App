package app.web.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.dto.*;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Users u = userService.register(request.getName(), request.getEmail(), request.getPassword());
        // send OTP
        authService.sendOtp(request.getEmail());
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "User registered. OTP sent to email",
                "data", Map.of("user", Map.of("id", u.getId(), "email", u.getEmail(), "name", u.getName()))));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpRequest request) {
        boolean ok = authService.verifyOtp(request.getEmail(), request.getCode());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired OTP"));
        }

        var userOpt = userService.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            userService.markVerified(userOpt.get());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Email verified"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginWithEmailAndPassword(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful",
                "data", Map.of("token", token)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(
            @Valid @RequestBody Map<String, @jakarta.validation.constraints.Email String> body) {
        String email = body.get("email");
        authService.sendOtp(email);
        return ResponseEntity.ok(Map.of("success", true, "message", "OTP resent"));
    }
}