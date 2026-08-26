package app.web.inventory.auth.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import app.web.inventory.auth.exception.OtpResendRateLimitException;

/**
 * Independent cooldown ladder for OTP *resend requests* — separate from OtpService's
 * per-OTP attempt lock (wrong-guess brute force). The two are intentionally decoupled:
 * an attacker burning a victim's attempt-lock must never block the victim from calling
 * /resend-otp and getting a fresh OTP. This class only governs how often a NEW OTP may
 * be issued to a given email; it says nothing about guesses against the current OTP.
 *
 * The ladder is an explicit, non-uniform list of step durations (not a formula), since
 * the target sequence (60s, 2m, 5m, 1hr, 1day) doesn't fit a clean multiplier. Strike N
 * uses steps[min(N, steps.size()) - 1]; once strikes exceed the list length, the delay
 * stays pinned at the last (largest) step — no auto-reset until the strike window
 * expires from inactivity or the account is verified.
 */
@Service
public class OtpResendRateLimiter {

    private static final String STRIKES_PREFIX = "otp_resend_strikes:";
    private static final String LOCK_PREFIX = "otp_resend_lock:";

    private final StringRedisTemplate redisTemplate;
    private final List<Long> stepSeconds;
    private final Duration strikeWindow;

    public OtpResendRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.otp-resend.step-seconds:60,120,300,3600,86400}") List<Long> stepSeconds,
            @Value("${app.otp-resend.strike-window-hours:48}") long strikeWindowHours) {
        if (stepSeconds == null || stepSeconds.isEmpty()) {
            throw new IllegalArgumentException("app.otp-resend.step-seconds must have at least one step");
        }
        this.redisTemplate = redisTemplate;
        this.stepSeconds = List.copyOf(stepSeconds);
        this.strikeWindow = Duration.ofHours(strikeWindowHours);
    }

    /**
     * Call before issuing a resend OTP. Throws if this email is currently locked out.
     */
    public void checkAllowed(String normalizedEmail) {
        String lockKey = LOCK_PREFIX + normalizedEmail;
        Long remaining = redisTemplate.getExpire(lockKey);
        if (remaining != null && remaining > 0) {
            throw new OtpResendRateLimitException(
                    "You've requested too many OTPs. Please try again later.", remaining);
        }
    }

    /**
     * Call every time a resend request is accepted (i.e. right after checkAllowed passes
     * and a new OTP is about to be generated). Advances the ladder and sets the next lock.
     */
    public void recordResend(String normalizedEmail) {
        String strikesKey = STRIKES_PREFIX + normalizedEmail;
        String lockKey = LOCK_PREFIX + normalizedEmail;

        Long strikes = redisTemplate.opsForValue().increment(strikesKey);
        if (strikes == null) {
            strikes = 1L;
        }
        if (strikes == 1L) {
            redisTemplate.expire(strikesKey, strikeWindow);
        }

        long delaySeconds = stepFor(strikes);
        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(delaySeconds));
    }

    /**
     * Call on successful verification. Clears the ladder so a freshly verified (or
     * re-registering-after-expiry) user doesn't inherit stale lockout state.
     */
    public void clear(String normalizedEmail) {
        redisTemplate.delete(STRIKES_PREFIX + normalizedEmail);
        redisTemplate.delete(LOCK_PREFIX + normalizedEmail);
    }

    private long stepFor(long strikeNumber) {
        int index = (int) Math.min(strikeNumber, stepSeconds.size()) - 1;
        return stepSeconds.get(index);
    }
}