package app.web.inventory.service;

import org.springframework.stereotype.Service;

import app.web.inventory.model.Otp;
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

    /**
     * Generate and send an OTP to the user email
     */
    public void sendOtp(String email) {
        Otp otp = otpService.createOtpFor(email);
        emailService.sendOtp(email, otp.getCode());
    }

    /**
     * Verify OTP for a given email
     */
    public boolean verifyOtp(String email, String code) {
        boolean verified = otpService.verify(email, code);
        if (verified) {
            userService.findByEmail(email).ifPresent(userService::markVerified);
        }
        return verified;
    }

    /**
     * Login with email + password and return JWT if successful
     */
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

    /**
     * Generate JWT for a given user
     */
    public String generateTokenForUser(Users user) {
        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }
}
