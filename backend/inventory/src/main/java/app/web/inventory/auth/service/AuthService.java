package app.web.inventory.auth.service;

import app.web.inventory.user.service.UserService;
import org.springframework.stereotype.Service;

import app.web.inventory.shared.exception.DuplicateResourceException;
import app.web.inventory.user.model.Users;
import app.web.inventory.auth.security.JwtUtil;

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

    public Users register(String name, String email, String rawPassword) {
        String normalizedEmail = UserService.normalizeEmail(email);
        Users existingUser = userService.findByEmail(normalizedEmail).orElse(null);

        if (existingUser != null && existingUser.isVerified()) {
            throw new DuplicateResourceException(
                    "An account already exists with this email. Please log in.");
        }

        String code = otpService.createOtpFor(normalizedEmail);

        Users user;
        if (existingUser == null) {
            user = userService.createLocalUser(name, normalizedEmail, rawPassword);
        } else {
            user = userService.updatePendingRegistration(existingUser, name, rawPassword);
        }
        emailService.sendOtp(normalizedEmail, code);

        return user;
    }

    /** Resend an OTP for an existing registration. */
    public void sendOtp(String email) {
        String normalizedEmail = UserService.normalizeEmail(email);
        String code = otpService.createOtpFor(normalizedEmail);
        emailService.sendOtp(normalizedEmail, code);
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