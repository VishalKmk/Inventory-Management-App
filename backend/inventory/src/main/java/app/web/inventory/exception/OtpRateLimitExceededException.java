package app.web.inventory.exception;

public class OtpRateLimitExceededException extends RuntimeException {
    public OtpRateLimitExceededException(String message) {
        super(message);
    }
}