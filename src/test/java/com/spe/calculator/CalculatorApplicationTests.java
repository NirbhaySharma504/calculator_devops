package com.spe.calculator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: {@code @SpringBootTest} starts the full application context.
 * The test body is empty on purpose - if any bean is misconfigured or a
 * dependency is missing, startup fails and so does this test.
 */
@SpringBootTest
class CalculatorApplicationTests {

	@Test
	void contextLoads() {
	}

}
