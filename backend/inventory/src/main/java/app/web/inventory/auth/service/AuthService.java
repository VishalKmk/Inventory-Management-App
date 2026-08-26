package app.web.inventory.auth.service;

import app.web.inventory.user.service.UserService;
import org.springframework.stereotype.Service;

import app.web.inventory.auth.exception.UnverifiedRegistrationExistsException;
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
    private final OtpResendRateLimiter otpResendRateLimiter;

    public AuthService(UserService userService, OtpService otpService, EmailService emailService, JwtUtil jwtUtil,
                       LoginRateLimiter loginRateLimiter, OtpResendRateLimiter otpResendRateLimiter) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.loginRateLimiter = loginRateLimiter;
        this.otpResendRateLimiter = otpResendRateLimiter;
    }

    public Users register(String name, String email, String rawPassword) {
        String normalizedEmail = UserService.normalizeEmail(email);
        Users existingUser = userService.findByEmail(normalizedEmail).orElse(null);

        if (existingUser != null) {
            if (existingUser.isVerified()) {
                throw new DuplicateResourceException(
                        "An account already exists with this email. Please log in.");
            }
            throw new UnverifiedRegistrationExistsException(
                    "A user has already registered with this email. "
                            + "If not verified then request an OTP or contact support.");
        }

        Users user = userService.createLocalUser(name, normalizedEmail, rawPassword);

        String code = otpService.createOtpFor(normalizedEmail);
        emailService.sendOtp(normalizedEmail, code);

        return user;
    }

    public ResendOutcome resendOtp(String email) {
        String normalizedEmail = UserService.normalizeEmail(email);

        Users user = userService.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.isVerified()) {
            return ResendOutcome.ACCEPTED_NO_ACTION;
        }

        otpResendRateLimiter.checkAllowed(normalizedEmail);

        String code = otpService.createOtpFor(normalizedEmail);
        otpResendRateLimiter.recordResend(normalizedEmail);
        emailService.sendOtp(normalizedEmail, code);

        return ResendOutcome.SENT;
    }

    public enum ResendOutcome {
        SENT,
        ACCEPTED_NO_ACTION
    }

    public int verifyOtp(String email, String code) {
        String normalizedEmail = UserService.normalizeEmail(email);
        int remaining = otpService.verify(normalizedEmail, code);
        userService.findByEmail(normalizedEmail).ifPresent(userService::markVerified);
        otpResendRateLimiter.clear(normalizedEmail);
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