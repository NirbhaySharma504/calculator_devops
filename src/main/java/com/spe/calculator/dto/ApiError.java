package com.spe.calculator.dto;

/**
 * Structured error body returned for every 4xx/5xx, e.g.
 * {"timestamp":"...","status":400,"error":"Bad Request","message":"Division by zero is not allowed","path":"/api/v1/divide"}.
 */
public record ApiError(String timestamp, int status, String error, String message, String path) {
}
