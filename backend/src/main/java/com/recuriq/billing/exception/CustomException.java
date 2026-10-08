package com.recuriq.billing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception.
 * Carries an HTTP status so GlobalExceptionHandler can map it correctly.
 */
@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public CustomException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    // Convenience factories ------------------------------------------------

    public static CustomException notFound(String message) {
        return new CustomException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static CustomException internalServerError(String message) {
        return new CustomException(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    public static CustomException conflict(String message) {
        return new CustomException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static CustomException unauthorized(String message) {
        return new CustomException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static CustomException forbidden(String message) {
        return new CustomException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static CustomException badRequest(String message) {
        return new CustomException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static CustomException paymentFailed(String message) {
        return new CustomException(message, HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED");
    }
}
