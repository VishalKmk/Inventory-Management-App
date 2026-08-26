package app.web.inventory.auth.exception;

public class OtpResendRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public OtpResendRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}