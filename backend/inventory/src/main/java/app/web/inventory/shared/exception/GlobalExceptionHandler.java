package app.web.inventory.shared.exception;

import app.web.inventory.auth.exception.*;
import app.web.inventory.shared.dto.ApiResponse;
import app.web.inventory.auth.dto.OtpVerifyResponseDto;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("This resource no longer exists. Please refresh."));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT) // Or 400, strictly 409 is for conflict
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<OtpVerifyResponseDto>> handleOtpExpired(OtpExpiredException ex) {
        OtpVerifyResponseDto dto = new OtpVerifyResponseDto(false, 0, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), dto));
    }

    @ExceptionHandler(InvalidOtpFormatException.class)
    public ResponseEntity<ApiResponse<OtpVerifyResponseDto>> handleInvalidOtpFormat(InvalidOtpFormatException ex) {
        OtpVerifyResponseDto dto = new OtpVerifyResponseDto(false, ex.getAttemptsRemaining(), ex.isLocked());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), dto));
    }

    @ExceptionHandler(OtpMismatchException.class)
    public ResponseEntity<ApiResponse<OtpVerifyResponseDto>> handleOtpMismatch(OtpMismatchException ex) {
        OtpVerifyResponseDto dto = new OtpVerifyResponseDto(false, ex.getAttemptsRemaining(), false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), dto));
    }

    @ExceptionHandler(OtpLockedException.class)
    public ResponseEntity<ApiResponse<OtpVerifyResponseDto>> handleOtpLocked(OtpLockedException ex) {
        OtpVerifyResponseDto dto = new OtpVerifyResponseDto(false, 0, true);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiResponse<>(false, ex.getMessage(), dto));
    }

    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpRateLimitExceeded(OtpRateLimitExceededException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(429).body(response); // HTTP 429 Too Many Requests
    }

    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleLoginRateLimitExceeded(LoginRateLimitException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}