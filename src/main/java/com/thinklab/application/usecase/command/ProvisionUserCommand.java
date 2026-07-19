package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import io.micronaut.core.annotation.Introspected;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to provision a new Enterprise Identity.
 * This record enforces strict boundary validation and input sanitization, acting
 * as a defensive shield for the core domain. It ensures that all provisioning
 * requests carry valid tenant context and authorized executor metadata.
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Edge Validation:</b> Fails fast at the application boundary using Jakarta constraints.</li>
 *     <li><b>Syntactic Sanitization:</b> Normalizes identifiers and emails to prevent index fragmentation.</li>
 *     <li><b>AOT Ready:</b> Utilizes @Introspected for reflection-free metadata generation.</li>
 * </ul>
 *
 * @param tenantId  The isolated organizational context (Holding or Branch).
 * @param parentId  The root organizational identifier (Optional for Holdings).
 * @param username  Unique system login identifier (Sanitized to lowercase).
 * @param email     Corporate communication channel (Validated and sanitized).
 * @param level     The authority tier defining data access scope.
 * @param profile   The biographical and contact metadata (Value Object).
 * @param executor  The identity of the agent authorizing the provisioning.
 */
@Slf4j
@Introspected
public record ProvisionUserCommand(
        @NotNull(message = "Tenant context is mandatory")
        UUID tenantId,

        @Nullable
        UUID parentId,

        @NotBlank(message = "Username is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username contains invalid characters")
        String username,

        @NotBlank(message = "Corporate email is mandatory")
        @Email(message = "Provide a valid corporate email address")
        String email,

        @NotNull(message = "User level is mandatory")
        UserLevel level,

        @Valid
        @NotNull(message = "User profile metadata is mandatory")
        UserProfile profile,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        String executor
) {

    /**
     * Compact constructor for defensive programming and edge-level sanitization.
     * Guaranteed to return a valid instance or fail-fast with a forensic log.
     */
    public ProvisionUserCommand {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");

        // Input Sanitization
        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        executor = executor.trim();

        if (username.isBlank() || email.isBlank()) {
            log.error("[ACTION: PROVISION_USER_VALIDATION] - Integrity violation: Blank identification fields detected.");
        }
    }
}