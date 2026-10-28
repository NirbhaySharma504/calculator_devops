package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidOperationException;
import org.springframework.stereotype.Service;

/**
 * Pure business logic of the calculator. Kept free of any web concerns so it
 * can be unit-tested without starting Spring.
 */
@Service
public class CalculatorService {

    public double add(double a, double b) {
        requireFinite(a, b);
        return checkResult(a + b);
    }

    public double subtract(double a, double b) {
        requireFinite(a, b);
        return checkResult(a - b);
    }

    public double multiply(double a, double b) {
        requireFinite(a, b);
        return checkResult(a * b);
    }

    public double divide(double a, double b) {
        requireFinite(a, b);
        if (b == 0) {
            throw new InvalidOperationException("Division by zero is not allowed");
        }
        return checkResult(a / b);
    }

    public double modulo(double a, double b) {
        requireFinite(a, b);
        if (b == 0) {
            throw new InvalidOperationException("Modulo by zero is not allowed");
        }
        return checkResult(a % b);
    }

    public double power(double a, double b) {
        requireFinite(a, b);
        return checkResult(Math.pow(a, b));
    }

    public double sqrt(double a) {
        requireFinite(a);
        if (a < 0) {
            throw new InvalidOperationException("Square root of a negative number is not allowed");
        }
        return Math.sqrt(a);
    }

    /** Rejects NaN / Infinity, which Spring happily parses from "NaN" or "Infinity". */
    private static void requireFinite(double... values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                throw new InvalidOperationException("Inputs must be finite numbers");
            }
        }
    }

    /** Rejects results that overflowed (e.g. 10^400) or are undefined (e.g. (-8)^0.5). */
    private static double checkResult(double result) {
        if (!Double.isFinite(result)) {
            throw new InvalidOperationException("Result is out of range or undefined");
        }
        return result;
    }
}
