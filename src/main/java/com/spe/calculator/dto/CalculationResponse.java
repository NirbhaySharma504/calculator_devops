package com.spe.calculator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Successful response, e.g. {"operation":"add","a":2.0,"b":3.0,"result":5.0}.
 * {@code b} is omitted for single-operand operations such as sqrt.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculationResponse(String operation, double a, Double b, double result) {
}
