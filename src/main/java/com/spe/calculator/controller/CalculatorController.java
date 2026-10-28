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
 */
@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculator;

    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

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

    @GetMapping("/sqrt")
    public CalculationResponse sqrt(@RequestParam double a) {
        return new CalculationResponse("sqrt", a, null, calculator.sqrt(a));
    }
}
