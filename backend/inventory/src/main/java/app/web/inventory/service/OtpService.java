package app.web.inventory.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import app.web.inventory.exception.InvalidOtpFormatException;
import app.web.inventory.exception.OtpExpiredException;
import app.web.inventory.exception.OtpLockedException;
import app.web.inventory.exception.OtpMismatchException;
import app.web.inventory.exception.OtpRateLimitExceededException;

@Service
public class OtpService {

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp_attempts:";
    private static final String RESEND_LIMIT_KEY_PREFIX = "otp_resend_limit:";

    private static final int MAX_ATTEMPTS = 5; // max incorrect attempts before locking
    private static final int MAX_DAILY_RESENDS = 2; // max resends per day limit
    private static final Pattern SIX_DIGIT_PATTERN = Pattern.compile("^\\d{6}$");

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final SecureRandom rnd = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate,
            @Value("${app.otp.ttl-minutes:10}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String createOtpFor(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        String normalizedEmail = UserService.normalizeEmail(email);

        // Check and enforce the 1-day rate limit for resending/generating OTPs
        checkAndIncrementResendLimit(normalizedEmail);

        String otpKey = OTP_KEY_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + normalizedEmail;

        String code = String.format("%06d", rnd.nextInt(1_000_000));

        redisTemplate.opsForValue().set(otpKey, code, ttl);
        redisTemplate.delete(attemptsKey);

        return code;
    }

    private void checkAndIncrementResendLimit(String normalizedEmail) {
        String limitKey = RESEND_LIMIT_KEY_PREFIX + normalizedEmail;

        Long count = redisTemplate.opsForValue().increment(limitKey);

        // If this is the very first request, set its expiration window to exactly 24
        // hours
        if (count != null && count == 1L) {
            redisTemplate.expire(limitKey, Duration.ofDays(1));
        }

        if (count != null && count > MAX_DAILY_RESENDS) {
            throw new OtpRateLimitExceededException("Daily OTP request limit reached. Please try again tomorrow.");
        }
    }

    public int verify(String email, String code) {
        String normalizedEmail = UserService.normalizeEmail(email);
        String otpKey = OTP_KEY_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + normalizedEmail;

        String stored = redisTemplate.opsForValue().get(otpKey);
        if (stored == null) {
            throw new OtpExpiredException("OTP expired or not requested.");
        }

        boolean wellFormed = code != null && SIX_DIGIT_PATTERN.matcher(code).matches();

        if (!wellFormed) {
            String reason = formatErrorMessage(code);
            burnAttemptOrThrowMismatch(otpKey, attemptsKey, reason, true);
            return -1;
        }

        if (stored.equals(code)) {
            long usedAttempts = readUsedAttempts(attemptsKey);
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            return (int) Math.max(0, MAX_ATTEMPTS - usedAttempts);
        }

        burnAttemptOrThrowMismatch(otpKey, attemptsKey, "Incorrect OTP.", false);
        return -1;
    }

    private void burnAttemptOrThrowMismatch(String otpKey, String attemptsKey, String message, boolean isFormatError) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, ttl);
        }
        long usedAttempts = attempts == null ? 1L : attempts;

        if (usedAttempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            throw new OtpLockedException("Too many incorrect attempts. Please request a new OTP.");
        }

        int remaining = (int) Math.max(0, MAX_ATTEMPTS - usedAttempts);
        if (isFormatError) {
            throw new InvalidOtpFormatException(message, remaining, false);
        }
        throw new OtpMismatchException(message, remaining);
    }

    private long readUsedAttempts(String attemptsKey) {
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        return attemptsStr == null ? 0L : Long.parseLong(attemptsStr);
    }

    private String formatErrorMessage(String code) {
        if (code == null || code.isBlank()) {
            return "OTP can not be empty.";
        }
        if (!code.chars().allMatch(Character::isDigit)) {
            return "OTP must be a number.";
        }
        return "OTP can not be less or more than 6 digits.";
    }
}