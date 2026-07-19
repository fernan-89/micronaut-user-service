package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to transition the operational
 * state of an Enterprise Identity.
 * This record acts as an input boundary, ensuring that lifecycle mutations
 * are authorized, justified, and carry full forensic context before
 * reaching the Domain State Machine.
 *
 * <p>Following the Thinklab Engineering Blueprint, this command separates
 * security-critical status changes from biographical updates to prevent
 * unauthorized privilege escalation.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Forensic Integrity:</b> Mandatory business justification for every state change.</li>
 *     <li><b>State Machine Governance:</b> Ensures targets are well-formed before domain processing.</li>
 *     <li><b>Reactive Immutability:</b> Thread-safe record structure for non-blocking pipelines.</li>
 * </ul>
 *
 * @param userId   The unique system identifier (UUID) of the target identity.
 * @param status   The intended target {@link UserStatus} (e.g., SUSPENDED, BLOCKED).
 * @param executor The identity of the administrative agent authorizing the transition.
 * @param reason   The mandatory business justification for the lifecycle event.
 */
@Slf4j
@Introspected
public record UpdateUserStatusCommand(
        @NotNull(message = "User ID is mandatory for lifecycle operations")
        UUID userId,

        @NotNull(message = "Target status is mandatory")
        UserStatus status,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        String executor,

        @NotBlank(message = "A business reason is mandatory for forensic compliance")
        @Size(max = 500, message = "Justification exceeds the safety limit of 500 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming and edge-level validation.
     * Guaranteed to log violations immediately at the application boundary.
     */
    public UpdateUserStatusCommand {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Input Sanitization
        executor = executor.trim();
        reason = reason.trim();

        if (executor.isBlank() || reason.isBlank()) {
            log.error("[ACTION: UPDATE_USER_STATUS_VALIDATION] [ID: {}] - Integrity violation: Blank compliance fields detected.", userId);
        }
    }
}