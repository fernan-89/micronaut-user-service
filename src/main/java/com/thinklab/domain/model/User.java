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

    // --- Métodos de Mudança de Estado (Domain Driven Design) ---

    public User updateProfile(@Nonnull UserProfile newProfile, @Nonnull String executor) {
        return new User(id, tenantId, parentId, username, level, status, roles, newProfile,
                failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }

    public User updateRoles(@Nonnull List<String> newRoles, @Nonnull String executor, @Nonnull String reason) {
        return new User(id, tenantId, parentId, username, level, status, newRoles, profile,
                failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }

    public User transitionTo(@Nonnull UserStatus newStatus, @Nonnull String executor, @Nonnull String reason) {
        this.status.validateTransitionTo(newStatus);
        return new User(id, tenantId, parentId, username, level, newStatus, roles, profile,
                failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }

    // --- Métodos de Fábrica e Estado ---

    public static User provision(@Nonnull UUID tenantId, @Nullable UUID parentId, @Nonnull String username,
                                 @Nonnull UserLevel level, @Nonnull UserProfile profile, @Nonnull String executor) {
        return new User(UUID.randomUUID(), tenantId, parentId, username, level, UserStatus.PENDING_ACTIVATION,
                Collections.emptyList(), profile, 0, false, executor, Instant.now(), null, null, 0L);
    }

    public User activate(@Nonnull String executor) {
        this.status.validateTransitionTo(UserStatus.ACTIVE);
        return new User(id, tenantId, parentId, username, level, UserStatus.ACTIVE, roles, profile,
                failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }

    public User recordFailedLogin(int threshold) {
        int newAttempts = this.failedAttempts + 1;
        UserStatus newStatus = newAttempts >= threshold ? UserStatus.BLOCKED : this.status;
        return new User(id, tenantId, parentId, username, level, newStatus, roles, profile,
                newAttempts, mfaEnabled, createdBy, createdAt, "system.security.agent", Instant.now(), version);
    }

    public User revoke(@Nonnull String executor) {
        this.status.validateTransitionTo(UserStatus.REVOKED);
        return new User(id, tenantId, parentId, username, level, UserStatus.REVOKED, roles, profile,
                failedAttempts, mfaEnabled, createdBy, createdAt, executor, Instant.now(), version);
    }
}