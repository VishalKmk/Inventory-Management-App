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
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserService userService, OtpService otpService, EmailService emailService, JwtUtil jwtUtil,
                       LoginRateLimiter loginRateLimiter) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.loginRateLimiter = loginRateLimiter;
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

    // Resend an OTP for an existing registration.
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

    public String loginWithEmailAndPassword(String email, String rawPassword, String clientIp) {
        String normalizedEmail = UserService.normalizeEmail(email);

        loginRateLimiter.checkAllowed(clientIp, normalizedEmail);

        Users user = userService.findByEmail(normalizedEmail).orElse(null);

        if (user == null || !userService.checkPassword(user, rawPassword)) {
            loginRateLimiter.recordFailure(clientIp, normalizedEmail);
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("Email not verified");
        }

        loginRateLimiter.recordSuccess(normalizedEmail);
        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }

    public String generateTokenForUser(Users user) {
        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }
}