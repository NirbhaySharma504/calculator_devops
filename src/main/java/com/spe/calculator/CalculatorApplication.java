package com.spe.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Calculator REST API.
 *
 * <p>{@code @SpringBootApplication} combines three annotations:
 * <ul>
 *   <li>{@code @Configuration} - this class can define Spring beans;</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring Boot configures Tomcat, Jackson (JSON),
 *       Spring MVC and Actuator automatically from the dependencies in pom.xml;</li>
 *   <li>{@code @ComponentScan} - every {@code @RestController}, {@code @Service} and
 *       {@code @RestControllerAdvice} under {@code com.spe.calculator} is found and registered.</li>
 * </ul>
 *
 * <p>The embedded Tomcat server means the whole app ships as one executable JAR
 * ({@code java -jar calculator.jar}), which is exactly what the Docker image runs.
 */
@SpringBootApplication
public class CalculatorApplication {

	public static void main(String[] args) {
		// Starts the Spring container and the embedded web server (port from SERVER_PORT, default 8080).
		SpringApplication.run(CalculatorApplication.class, args);
	}

}
