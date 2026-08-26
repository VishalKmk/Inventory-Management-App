package app.web.inventory.auth.exception;

public class UnverifiedRegistrationExistsException extends RuntimeException {

    public UnverifiedRegistrationExistsException(String message) {
        super(message);
    }
}