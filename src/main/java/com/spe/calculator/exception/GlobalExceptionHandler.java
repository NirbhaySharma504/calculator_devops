package com.spe.calculator.exception;

import com.spe.calculator.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Converts exceptions into the structured {@link ApiError} JSON body.
 *
 * <p>{@code @RestControllerAdvice} makes this class apply to every controller:
 * when a request handler throws, Spring looks for an {@code @ExceptionHandler}
 * method here whose parameter type matches the exception and uses its return
 * value as the HTTP response. This keeps try/catch blocks out of the controller
 * and guarantees every error has the same JSON shape.
 *
 * <p>All three cases are client mistakes, so they map to 400 Bad Request (not 500,
 * which would mean the server itself is broken).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Division by zero, sqrt of a negative, overflow, NaN/Infinity input. */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiError> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    /**
     * e.g. /api/v1/add?a=2 (no b). Thrown by Spring itself, before the controller
     * method runs, because {@code @RequestParam} is required by default.
     * If both a and b are missing, Spring reports the first one it checks ('a').
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Missing required parameter '" + ex.getParameterName() + "'", req);
    }

    /** e.g. /api/v1/add?a=abc&b=1. Thrown by Spring when "abc" cannot be converted to a double. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' must be a number, got '" + ex.getValue() + "'", req);
    }

    /** Logs the rejected request and wraps the error in a ResponseEntity with the given status. */
    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        log.warn("{} {} -> {} ({})", req.getMethod(), req.getRequestURI(), status.value(), message);
        ApiError body = new ApiError(Instant.now().toString(), status.value(),
                status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
