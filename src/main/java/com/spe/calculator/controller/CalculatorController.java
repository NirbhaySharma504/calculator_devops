package com.spe.calculator.controller;

import com.spe.calculator.dto.CalculationResponse;
import com.spe.calculator.service.CalculatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints: GET /api/v1/{operation}?a=..&b=..
 * Parameters are primitive doubles, so a missing or non-numeric value is
 * rejected by Spring and turned into a 400 by GlobalExceptionHandler.
 *
 * <p>This class is only the web layer: it reads the query parameters, calls
 * {@link CalculatorService} and wraps the result. No maths happens here.
 */
// @RestController = @Controller + @ResponseBody: every return value is written
// straight to the HTTP response as JSON (by Jackson) instead of rendering a view.
@RestController
// Common URL prefix for all endpoints. The "v1" lets a future v2 API live side by side.
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculator;

    // Constructor injection: Spring sees the single constructor and passes in the
    // CalculatorService bean. The field can be final and tests can pass their own instance.
    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

    // @GetMapping("/add") -> handles GET /api/v1/add.
    // @RequestParam double a -> reads ?a= from the URL and converts it to a double.
    // The parameter is required by default: if it is absent Spring throws
    // MissingServletRequestParameterException, if it is not a number it throws
    // MethodArgumentTypeMismatchException. Both become HTTP 400 in GlobalExceptionHandler.
    @GetMapping("/add")
    public CalculationResponse add(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("add", a, b, calculator.add(a, b));
    }

    @GetMapping("/subtract")
    public CalculationResponse subtract(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("subtract", a, b, calculator.subtract(a, b));
    }

    @GetMapping("/multiply")
    public CalculationResponse multiply(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("multiply", a, b, calculator.multiply(a, b));
    }

    // Division by zero is detected in the service, which throws InvalidOperationException -> 400.
    @GetMapping("/divide")
    public CalculationResponse divide(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("divide", a, b, calculator.divide(a, b));
    }

    @GetMapping("/modulo")
    public CalculationResponse modulo(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("modulo", a, b, calculator.modulo(a, b));
    }

    @GetMapping("/power")
    public CalculationResponse power(@RequestParam double a, @RequestParam double b) {
        return new CalculationResponse("power", a, b, calculator.power(a, b));
    }

    // Single-operand operation: b is passed as null, so it is left out of the JSON
    // (see @JsonInclude(NON_NULL) on CalculationResponse).
    @GetMapping("/sqrt")
    public CalculationResponse sqrt(@RequestParam double a) {
        return new CalculationResponse("sqrt", a, null, calculator.sqrt(a));
    }
}
