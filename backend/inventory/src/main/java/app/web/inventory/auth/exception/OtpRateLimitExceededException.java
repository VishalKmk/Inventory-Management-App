package app.web.inventory.auth.exception;

public class OtpRateLimitExceededException extends RuntimeException {
    public OtpRateLimitExceededException(String message) {
        super(message);
    }
}