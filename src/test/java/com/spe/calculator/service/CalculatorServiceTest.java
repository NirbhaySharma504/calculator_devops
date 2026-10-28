package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidOperationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Plain JUnit 5 unit tests: no Spring context, so they run in milliseconds. */
class CalculatorServiceTest {

    private static final double DELTA = 1e-9;

    private final CalculatorService calculator = new CalculatorService();

    @Nested
    @DisplayName("add")
    class Add {
        @ParameterizedTest(name = "{0} + {1} = {2}")
        @CsvSource({"2, 3, 5", "-2, -3, -5", "2.5, 0.5, 3", "0, 0, 0", "-7, 7, 0"})
        void addsTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, calculator.add(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {
        @ParameterizedTest(name = "{0} - {1} = {2}")
        @CsvSource({"5, 3, 2", "3, 5, -2", "-2, -3, 1", "1.5, 0.5, 1"})
        void subtractsTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, calculator.subtract(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("multiply")
    class Multiply {
        @ParameterizedTest(name = "{0} * {1} = {2}")
        @CsvSource({"4, 5, 20", "-4, 5, -20", "-4, -5, 20", "7, 0, 0", "0.5, 0.5, 0.25"})
        void multipliesTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, calculator.multiply(a, b), DELTA);
        }

        @Test
        void rejectsOverflow() {
            assertThrows(InvalidOperationException.class, () -> calculator.multiply(Double.MAX_VALUE, 10));
        }
    }

    @Nested
    @DisplayName("divide")
    class Divide {
        @ParameterizedTest(name = "{0} / {1} = {2}")
        @CsvSource({"10, 2, 5", "7, 2, 3.5", "-9, 3, -3", "0, 5, 0", "1, 3, 0.3333333333"})
        void dividesTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, calculator.divide(a, b), 1e-9);
        }

        @Test
        void rejectsDivisionByZero() {
            InvalidOperationException ex =
                    assertThrows(InvalidOperationException.class, () -> calculator.divide(5, 0));
            assertEquals("Division by zero is not allowed", ex.getMessage());
        }

        @Test
        void rejectsDivisionByNegativeZero() {
            assertThrows(InvalidOperationException.class, () -> calculator.divide(5, -0.0));
        }
    }

    @Nested
    @DisplayName("modulo")
    class Modulo {
        @ParameterizedTest(name = "{0} % {1} = {2}")
        @CsvSource({"10, 3, 1", "-10, 3, -1", "5.5, 2, 1.5", "4, 2, 0"})
        void computesRemainder(double a, double b, double expected) {
            assertEquals(expected, calculator.modulo(a, b), DELTA);
        }

        @Test
        void rejectsModuloByZero() {
            assertThrows(InvalidOperationException.class, () -> calculator.modulo(5, 0));
        }
    }

    @Nested
    @DisplayName("power")
    class Power {
        @ParameterizedTest(name = "{0} ^ {1} = {2}")
        @CsvSource({"2, 10, 1024", "5, 0, 1", "2, -1, 0.5", "9, 0.5, 3", "-2, 3, -8"})
        void raisesToPower(double a, double b, double expected) {
            assertEquals(expected, calculator.power(a, b), DELTA);
        }

        @Test
        void rejectsOverflow() {
            assertThrows(InvalidOperationException.class, () -> calculator.power(10, 400));
        }

        @Test
        void rejectsUndefinedResult() {
            // (-8)^0.5 is not a real number -> NaN
            assertThrows(InvalidOperationException.class, () -> calculator.power(-8, 0.5));
        }
    }

    @Nested
    @DisplayName("sqrt")
    class Sqrt {
        @ParameterizedTest(name = "sqrt({0}) = {1}")
        @CsvSource({"16, 4", "0, 0", "2, 1.4142135623730951", "0.25, 0.5"})
        void computesSquareRoot(double a, double expected) {
            assertEquals(expected, calculator.sqrt(a), DELTA);
        }

        @Test
        void rejectsNegativeInput() {
            assertThrows(InvalidOperationException.class, () -> calculator.sqrt(-1));
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {
        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void rejectsNonFiniteInputs(double bad) {
            assertThrows(InvalidOperationException.class, () -> calculator.add(bad, 1));
            assertThrows(InvalidOperationException.class, () -> calculator.subtract(1, bad));
            assertThrows(InvalidOperationException.class, () -> calculator.multiply(bad, 1));
            assertThrows(InvalidOperationException.class, () -> calculator.divide(1, bad));
            assertThrows(InvalidOperationException.class, () -> calculator.modulo(bad, 1));
            assertThrows(InvalidOperationException.class, () -> calculator.power(bad, 1));
            assertThrows(InvalidOperationException.class, () -> calculator.sqrt(bad));
        }
    }
}
