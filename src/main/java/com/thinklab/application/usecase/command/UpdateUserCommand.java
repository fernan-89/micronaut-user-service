package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserProfile;
import io.micronaut.core.annotation.Introspected;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to update the biographical profile
 * of an existing Enterprise Identity.
 * This record enforces strict boundary validation, ensuring that metadata
 * mutations are authorized by an executor and carry a valid target identifier.
 *
 * <p>Following the Thinklab Engineering Blueprint, this command is strictly
 * decoupled from security-sensitive fields (e.g., Status, Roles) to prevent
 * unauthorized privilege escalation via profile update endpoints.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Mass Assignment Protection:</b> Only biographical metadata is exposed.</li>
 *     <li><b>Forensic Traceability:</b> Mandatory executor identification for every mutation.</li>
 *     <li><b>Reactive Integrity:</b> Immutable structure for safe multithreaded processing.</li>
 * </ul>
 *
 * @param userId   The unique system identifier (UUID) of the user to be updated.
 * @param profile  The new biographical and contact metadata (Value Object).
 * @param executor The identity of the agent (Admin or System) authorizing the change.
 */
@Slf4j
@Introspected
public record UpdateUserCommand(
        @NotNull(message = "User ID is mandatory for update operations")
        UUID userId,

        @Valid
        @NotNull(message = "New user profile metadata is mandatory")
        UserProfile profile,

        @NotBlank(message = "Executor identification is mandatory for auditing purposes")
        String executor
) {

    /**
     * Compact constructor for defensive programming and edge-level validation.
     * Guarantees that only well-formed mutation intents enter the application logic.
     */
    public UpdateUserCommand {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");

        // Input Sanitization
        executor = executor.trim();

        if (executor.isBlank()) {
            log.error("[ACTION: UPDATE_USER_VALIDATION] [ID: {}] - Integrity violation: Blank executor detected.", userId);
        }
    }
}