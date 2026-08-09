package app.web.inventory.service;

import org.springframework.stereotype.Service;

import app.web.inventory.model.Users;
import app.web.inventory.security.JwtUtil;

@Service
public class AuthService {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, OtpService otpService, EmailService emailService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
    }

    public void sendOtp(String email) {
        String code = otpService.createOtpFor(email);
        emailService.sendOtp(email, code);
    }

    public int verifyOtp(String email, String code) {
        int remaining = otpService.verify(email, code);
        userService.findByEmail(email).ifPresent(userService::markVerified);
        return remaining;
    }

    public String loginWithEmailAndPassword(String email, String rawPassword) {
        Users user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!userService.checkPassword(user, rawPassword)) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("Email not verified");
        }

        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }

    public String generateTokenForUser(Users user) {
        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }
}