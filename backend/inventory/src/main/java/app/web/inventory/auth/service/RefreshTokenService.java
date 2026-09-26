package app.web.inventory.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import app.web.inventory.auth.exception.InvalidRefreshTokenException;

/**
 * Opaque (non-JWT) refresh tokens, stored server-side in Redis as
 * refresh_token:{token} -> userId, TTL-bound. Opaque rather than a second JWT
 * because it never needs to be parsed client-side and a store lookup lets us
 * revoke it early (rotation, logout) — a self-contained JWT can't be revoked
 * before its own expiry without an extra denylist doing the same job anyway.
 *
 * Rotation: every successful refresh deletes the presented token and issues a
 * new one. This bounds how long a leaked refresh token stays useful — reusing
 * an already-rotated token fails outright — at the cost of the client needing
 * to persist the new refresh token from every refresh response.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(refreshExpirationMs);
    }

    /** Issue and store a new refresh token for this user. */
    public String issue(UUID userId) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), ttl);
        return token;
    }

    /**
     * Validate a presented refresh token and rotate it: the old token is
     * deleted and a fresh one issued, both atomically enough for this use case
     * (a losing concurrent refresh on the same token simply fails below rather
     * than silently reusing a half-dead token).
     *
     * @return the resolved userId and the newly issued replacement token
     */
    public RotationResult rotate(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }

        String key = KEY_PREFIX + presentedToken;
        String userIdValue = redisTemplate.opsForValue().get(key);
        if (userIdValue == null) {
            throw new InvalidRefreshTokenException("Refresh token is invalid or expired. Please log in again.");
        }

        // Delete before issuing the replacement so a token can never be
        // rotated twice even if the two steps interleave with another request.
        redisTemplate.delete(key);

        UUID userId = UUID.fromString(userIdValue);
        String newToken = issue(userId);
        return new RotationResult(userId, newToken);
    }

    /** Revoke a refresh token outright (logout). Safe to call on an already-gone token. */
    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }

    public record RotationResult(UUID userId, String newRefreshToken) {
    }
}