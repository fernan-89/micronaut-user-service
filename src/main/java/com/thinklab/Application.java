package com.thinklab;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Main Entry Point: Bootstrap class for the Identity & Access Management (IAM) Service.
 * This class orchestrates the application startup using the Micronaut framework,
 * ensuring high scalability, low memory footprint, and non-blocking execution.
 *
 * <p>Following the Thinklab Engineering Blueprint, this class also serves as the
 * anchor for OpenAPI/Swagger documentation, generated at compile-time.</p>
 *
 * @author Thinklab Staff Engineering
 * @version 1.0.0
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Identity & Access Management (IAM) API",
                version = "1.0.0",
                description = "Reactive mission-critical service for managing users, roles, and hierarchical tenant-based identities.",
                contact = @Contact(name = "Thinklab Staff Engineering", email = "staff@thinklab.com"),
                license = @License(name = "Apache 2.0", url = "https://thinklab.com/licenses/LICENSE-2.0")
        )
)
public class Application {

    /**
     * Standard entry point for the JVM.
     * Starts the Micronaut runtime and initializes the reactive context.
     *
     * @param args Command line arguments passed during startup.
     */
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}