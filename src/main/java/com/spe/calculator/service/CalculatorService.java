package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidOperationException;
import org.springframework.stereotype.Service;

/**
 * Pure business logic of the calculator. Kept free of any web concerns so it
 * can be unit-tested without starting Spring.
 *
 * <p>Every operation follows the same pattern: validate the inputs, compute,
 * then validate the result. Any problem is reported by throwing
 * {@link InvalidOperationException}, which the web layer turns into HTTP 400.
 */
// @Service marks this class as a Spring bean: one shared instance is created at
// startup and injected into CalculatorController.
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
        // With doubles, 5 / 0 does not throw in Java: it silently returns Infinity.
        // We check explicitly so the client gets a clear 400 instead.
        // (b == 0 is also true for -0.0, so that case is covered as well.)
        if (b == 0) {
            throw new InvalidOperationException("Division by zero is not allowed");
        }
        return checkResult(a / b);
    }

    public double modulo(double a, double b) {
        requireFinite(a, b);
        // a % 0 with doubles returns NaN, so reject it up front with a clear message.
        if (b == 0) {
            throw new InvalidOperationException("Modulo by zero is not allowed");
        }
        return checkResult(a % b);
    }

    public double power(double a, double b) {
        requireFinite(a, b);
        // checkResult catches both overflow (10^400 = Infinity) and
        // undefined results ((-8)^0.5 = NaN).
        return checkResult(Math.pow(a, b));
    }

    public double sqrt(double a) {
        requireFinite(a);
        if (a < 0) {
            throw new InvalidOperationException("Square root of a negative number is not allowed");
        }
        // sqrt of a finite, non-negative number is always finite, so no checkResult needed.
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
