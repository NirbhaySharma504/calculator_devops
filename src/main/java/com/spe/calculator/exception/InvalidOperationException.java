package com.spe.calculator.exception;

/**
 * Thrown when an operation cannot produce a valid numeric result
 * (division by zero, square root of a negative number, overflow, NaN input...).
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
