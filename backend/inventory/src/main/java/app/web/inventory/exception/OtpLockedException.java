package app.web.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OtpLockedException extends RuntimeException {
    public OtpLockedException(String message) {
        super(message);
    }
}