package com.spe.calculator.dto;

/**
 * Structured error body returned for every 4xx/5xx, e.g.
 * {"timestamp":"...","status":400,"error":"Bad Request","message":"Division by zero is not allowed","path":"/api/v1/divide"}.
 *
 * <p>Every error has the same shape, so a client can always read {@code message}
 * to know what went wrong. Built by GlobalExceptionHandler.
 *
 * @param timestamp when the error happened (ISO-8601, UTC)
 * @param status    HTTP status code, e.g. 400
 * @param error     HTTP reason phrase, e.g. "Bad Request"
 * @param message   human-readable explanation of the problem
 * @param path      the request URI that failed
 */
public record ApiError(String timestamp, int status, String error, String message, String path) {
}
