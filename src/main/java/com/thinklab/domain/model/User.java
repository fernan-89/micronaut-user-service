package com.thinklab.domain.model;

import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.domain.valueobject.UserProfile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model: Aggregate Root representing an Enterprise Identity (User).
 * This record serves as the source of truth for user data and authority tiers,
 * ensuring that all state transitions are governed by strict business invariants.
 *
 * <p>Following the Thinklab Engineering Blueprint, this aggregate is immutable
 * and strictly decoupled from infrastructure/persistence concerns.</p>
 *
 * @param id             Unique system identifier (UUID).
 * @param tenantId       The isolated organizational context (Holding or Branch).
 * @param parentId       The root organizational identifier (Optional for Holdings).
 * @param username       Unique system login identifier.
 * @param level          The authority tier defining data access scope.
 * @param status         The current operational lifecycle state.
 * @param roles          Collection of functional permissions (e.g., BILLING_ADMIN).
 * @param profile        Biographical and contact identification metadata.
 * @param failedAttempts Counter for security policy enforcement.
 * @param mfaEnabled     Security flag for multi-factor authentication requirement.
 * @param createdBy      Identifier of the agent who provisioned the identity.
 * @param createdAt      UTC timestamp of initial creation.
 * @param updatedBy      Identifier of the last agent to modify this state.
 * @param updatedAt      UTC timestamp of the last mutation.
 * @param version        Optimistic locking version for reactive concurrency.
 */
public record User(
        @Nonnull UUID id,
        @Nonnull UUID tenantId,
        @Nullable UUID parentId,
        @Nonnull String username,
        @Nonnull UserLevel level,
        @Nonnull UserStatus status,
        @Nonnull List<String> roles,
        @Nonnull UserProfile profile,
        @Nonnull Integer failedAttempts,
        @Nonnull Boolean mfaEnabled,
        @Nonnull String createdBy,
        @Nonnull Instant createdAt,
        @Nullable String updatedBy,
        @Nullable Instant updatedAt,
        @Nonnull Long version
) {

    /**
     * Compact constructor to enforce domain invariants and prevent corrupted state.
     */
    public User {
        Objects.requireNonNull(id, "User ID is mandatory");
        Objects.requireNonNull(tenantId, "Tenant context is mandatory");
        Objects.requireNonNull(username, "Username is mandatory");
        Objects.requireNonNull(level, "Authority level is mandatory");
        Objects.requireNonNull(status, "Operational status is mandatory");
        Objects.requireNonNull(profile, "User profile is mandatory");
        Objects.requireNonNull(failedAttempts, "Failed attempts counter is mandatory");
        Objects.requireNonNull(mfaEnabled, "MFA flag is mandatory");
        Objects.requireNonNull(createdBy, "Creator identification is mandatory");
        Objects.requireNonNull(createdAt, "Creation timestamp is mandatory");
        Objects.requireNonNull(version, "Concurrency version is mandatory");

        roles = roles != null ? List.copyOf(roles) : Collections.emptyList();
        username = username.trim().toLowerCase();
    }

    /**
     * Factory: Provisions a new Identity in the initial state.
     */
    public static User provision(
            @Nonnull UUID tenantId,
            @Nullable UUID parentId,
            @Nonnull String username,
            @Nonnull UserLevel level,
            @Nonnull UserProfile profile,
            @Nonnull String executor
    ) {
        return new User(
                UUID.randomUUID(),
                tenantId,
                parentId,
                username,
                level,
                UserStatus.PENDING_ACTIVATION,
                Collections.emptyList(),
                profile,
                0,
                false,
                executor,
                Instant.now(),
                null,
                null,
                0L
        );
    }

    /**
     * Transitions the identity to ACTIVE state.
     */
    public User activate(@Nonnull String executor) {
        this.status.validateTransitionTo(UserStatus.ACTIVE);
        return new User(id, tenantId, parentId, username, level, UserStatus.ACTIVE, roles,
                profile, failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }

    /**
     * Records a failed login attempt and automatically blocks if the threshold is met.
     */
    public User recordFailedLogin(int threshold) {
        int newAttempts = this.failedAttempts + 1;
        UserStatus newStatus = newAttempts >= threshold ? UserStatus.BLOCKED : this.status;
        return new User(id, tenantId, parentId, username, level, newStatus, roles,
                profile, newAttempts, mfaEnabled, createdBy, createdAt, "system.security.agent", Instant.now(), version);
    }

    /**
     * Irreversibly revokes the identity (Irreversible Terminal State).
     */
    public User revoke(@Nonnull String executor) {
        this.status.validateTransitionTo(UserStatus.REVOKED);
        return new User(id, tenantId, parentId, username, level, UserStatus.REVOKED, roles,
                profile, failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }
}