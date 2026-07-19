package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to modify the functional roles
 * and permissions of an existing Enterprise Identity.
 * This record acts as an input boundary, ensuring that privilege mutations
 * are authorized, documented, and properly formatted before reaching the
 * domain's authorization engine.
 *
 * <p>Following the Thinklab Engineering Blueprint, role updates are treated
 * as security-sensitive events, requiring a mandatory business reason
 * for forensic audit compliance.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Least Privilege Integrity:</b> Validates that role lists are well-formed.</li>
 *     <li><b>Forensic Traceability:</b> Mandatory justification for authorization changes.</li>
 *     <li><b>Reactive Immutability:</b> Thread-safe structure for high-throughput pipelines.</li>
 * </ul>
 *
 * @param userId   The unique system identifier (UUID) of the target identity.
 * @param roles    The new collection of functional roles (e.g., BILLING_ADMIN, AUDITOR).
 * @param executor The identity of the administrative agent authorizing the change.
 * @param reason   The mandatory business justification for the privilege modification.
 */
@Slf4j
@Introspected
public record UpdateUserRolesCommand(
        @NotNull(message = "User ID is mandatory for role updates")
        UUID userId,

        @NotEmpty(message = "The roles collection cannot be empty")
        List<String> roles,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        String executor,

        @NotBlank(message = "A business reason is mandatory for forensic compliance")
        @Size(max = 500, message = "Justification exceeds the safety limit of 500 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming and input sanitization.
     * Guaranteed to return a valid instance or log a critical validation failure.
     */
    public UpdateUserRolesCommand {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(roles, "roles cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Input Sanitization
        roles = List.copyOf(roles); // Ensure immutability of the collection
        executor = executor.trim();
        reason = reason.trim();

        if (executor.isBlank() || reason.isBlank()) {
            log.error("[ACTION: UPDATE_USER_ROLES_VALIDATION] [ID: {}] - Integrity violation: Blank compliance fields detected.", userId);
        }
    }
}