package com.spe.calculator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Successful response, e.g. {"operation":"add","a":2.0,"b":3.0,"result":5.0}.
 * {@code b} is omitted for single-operand operations such as sqrt.
 *
 * <p>A Java {@code record} is an immutable data class: the compiler generates the
 * constructor, getters, equals/hashCode and toString. Jackson (the JSON library
 * bundled with Spring Web) turns each component into a JSON field of the same name.
 *
 * <p>{@code b} is a {@code Double} (object) rather than {@code double} (primitive)
 * so it can be null; {@code @JsonInclude(NON_NULL)} then drops it from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalculationResponse(String operation, double a, Double b, double result) {
}
