package com.spe.calculator.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC integration tests: the full application context is started and
 * real HTTP requests go through the DispatcherServlet, controller, service and
 * exception handler (MockMvc, no network socket needed).
 */
@SpringBootTest          // start the whole application context, as in production
@AutoConfigureMockMvc    // provide a MockMvc instance wired to that context
class CalculatorControllerIntegrationTest {

    // MockMvc sends simulated HTTP requests and lets us assert on status, headers and JSON.
    @Autowired
    private MockMvc mvc;

    // One test per binary operation, driven by the CSV rows below.
    // jsonPath("$.result") reads a field from the JSON response body.
    @ParameterizedTest(name = "GET /api/v1/{0}?a={1}&b={2} -> {3}")
    @CsvSource({
            "add,      2,  3,  5",
            "subtract, 10, 4,  6",
            "multiply, 6,  7,  42",
            "divide,   9,  2,  4.5",
            "modulo,   10, 3,  1",
            "power,    2,  8,  256"
    })
    void binaryOperationsReturnJsonResult(String op, double a, double b, double expected) throws Exception {
        mvc.perform(get("/api/v1/" + op).param("a", String.valueOf(a)).param("b", String.valueOf(b)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.operation").value(op))
                .andExpect(jsonPath("$.a").value(a))
                .andExpect(jsonPath("$.b").value(b))
                .andExpect(jsonPath("$.result").value(closeTo(expected, 1e-9)));
    }

    @Test
    void sqrtReturnsResultWithoutB() throws Exception {
        mvc.perform(get("/api/v1/sqrt").param("a", "81"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("sqrt"))
                .andExpect(jsonPath("$.result").value(9.0))
                .andExpect(jsonPath("$.b").doesNotExist());
    }

    // The brief's key error case: checks the status code and every field of the ApiError body.
    @Test
    void divideByZeroReturns400WithErrorBody() throws Exception {
        mvc.perform(get("/api/v1/divide").param("a", "5").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Division by zero is not allowed"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // The next tests cover input validation done by Spring before our code runs.
    @Test
    void missingParameterReturns400() throws Exception {
        mvc.perform(get("/api/v1/add").param("a", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'b'"));
    }

    @Test
    void nonNumericParameterReturns400() throws Exception {
        mvc.perform(get("/api/v1/multiply").param("a", "abc").param("b", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must be a number")));
    }

    @Test
    void nanInputReturns400() throws Exception {
        mvc.perform(get("/api/v1/add").param("a", "NaN").param("b", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Inputs must be finite numbers"));
    }

    @Test
    void sqrtOfNegativeReturns400() throws Exception {
        mvc.perform(get("/api/v1/sqrt").param("a", "-4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void powerOverflowReturns400() throws Exception {
        mvc.perform(get("/api/v1/power").param("a", "10").param("b", "400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Result is out of range or undefined"));
    }

    // The same endpoint Docker, Compose and Jenkins poll to decide if the app is healthy.
    @Test
    void actuatorHealthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
