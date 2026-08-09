package app.web.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOtpFormatException extends RuntimeException {
    private final int attemptsRemaining;
    private final boolean locked;

    public InvalidOtpFormatException(String message, int attemptsRemaining, boolean locked) {
        super(message);
        this.attemptsRemaining = attemptsRemaining;
        this.locked = locked;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public boolean isLocked() {
        return locked;
    }
}