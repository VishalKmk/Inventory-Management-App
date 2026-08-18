package app.web.inventory.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import app.web.inventory.auth.exception.LoginRateLimitException;

@Service
public class LoginRateLimiter {

    private static final String IP_STRIKES_PREFIX = "login_strikes:ip:";
    private static final String IP_LOCK_PREFIX = "login_lock:ip:";
    private static final String ACCOUNT_STRIKES_PREFIX = "login_strikes:acct:";
    private static final String ACCOUNT_LOCK_PREFIX = "login_lock:acct:";

    private final StringRedisTemplate redisTemplate;
    private final long baseSeconds;
    private final long maxSeconds;
    private final int maxStrikesTracked;
    private final Duration strikeWindow;

    public LoginRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.login-ratelimit.base-seconds:30}") long baseSeconds,
            @Value("${app.login-ratelimit.max-seconds:900}") long maxSeconds,
            @Value("${app.login-ratelimit.strike-window-minutes:30}") long strikeWindowMinutes) {
        this.redisTemplate = redisTemplate;
        this.baseSeconds = baseSeconds;
        this.maxSeconds = maxSeconds;
        this.strikeWindow = Duration.ofMinutes(strikeWindowMinutes);
        this.maxStrikesTracked = (int) (Math.log((double) maxSeconds / baseSeconds) / Math.log(2)) + 2;
    }

    // Call before verifying a password. Throws if either the IP or the account is currently locked out.
    public void checkAllowed(String ip, String normalizedEmail) {
        checkTrack(IP_LOCK_PREFIX, ip, "Too many failed login attempts from this network.");
        checkTrack(ACCOUNT_LOCK_PREFIX, normalizedEmail, "Too many failed login attempts for this account.");
    }

    // Call after a failed password check. Increments strikes and sets the next lockout.
    public void recordFailure(String ip, String normalizedEmail) {
        recordFailureForTrack(IP_STRIKES_PREFIX, IP_LOCK_PREFIX, ip);
        recordFailureForTrack(ACCOUNT_STRIKES_PREFIX, ACCOUNT_LOCK_PREFIX, normalizedEmail);
    }

    // Call after a successful login. Clears the account lock.
    public void recordSuccess(String normalizedEmail) {
        redisTemplate.delete(ACCOUNT_STRIKES_PREFIX + normalizedEmail);
        redisTemplate.delete(ACCOUNT_LOCK_PREFIX + normalizedEmail);
    }

    private void checkTrack(String lockPrefix, String key, String message) {
        if (key == null) {
            return;
        }
        String ttlKey = lockPrefix + key;
        Long remaining = redisTemplate.getExpire(ttlKey);
        if (remaining != null && remaining > 0) {
            throw new LoginRateLimitException(message, remaining);
        }
    }

    private void recordFailureForTrack(String strikesPrefix, String lockPrefix, String key) {
        if (key == null) {
            return;
        }
        String strikesKey = strikesPrefix + key;
        String lockKey = lockPrefix + key;

        Long strikes = redisTemplate.opsForValue().increment(strikesKey);
        if (strikes == null) {
            strikes = 1L;
        }
        if (strikes == 1L) {
            redisTemplate.expire(strikesKey, strikeWindow);
        }

        int capped = (int) Math.min(strikes, maxStrikesTracked);
        long delaySeconds = Math.min(maxSeconds, baseSeconds * (1L << Math.max(0, capped - 1)));

        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(delaySeconds));
    }
}