package app.web.inventory.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.*;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", errors,
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", errors,
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {} for request: {}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {} for request: {}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", "Invalid credentials",
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {} for request: {}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "Access denied",
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        // Log the full stack trace for debugging but don't expose it
        log.error("Runtime error occurred for request: {}", request.getDescription(false), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while processing your request",
                "timestamp", LocalDateTime.now(),
                "reference", UUID.randomUUID().toString() // For tracking in logs
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
        // Log the full stack trace for debugging but don't expose it
        log.error("Unexpected error occurred for request: {}", request.getDescription(false), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An unexpected error occurred",
                "timestamp", LocalDateTime.now(),
                "reference", UUID.randomUUID().toString() // For tracking in logs
        ));
    }
}
