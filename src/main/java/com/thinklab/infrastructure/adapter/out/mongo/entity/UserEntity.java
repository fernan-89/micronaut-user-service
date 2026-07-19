package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.Indexes;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Version;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Infrastructure Entity: Persistence model for Enterprise Identities (Users).
 * This record maps the {@link User} aggregate root directly to the MongoDB
 * "users" collection. It acts as an Anti-Corruption Layer (ACL), shielding
 * the Domain from database-specific metadata and BSON serialization concerns.
 *
 * <p><b>Persistence Principles:</b></p>
 * <ul>
 *     <li><b>Native UUIDs:</b> Stored as BSON Binary Subtype 4 for optimal indexing.</li>
 *     <li><b>Optimistic Locking:</b> Enforced via @Version to prevent concurrent update collisions.</li>
 *     <li><b>Multi-tenant Isolation:</b> Strictly indexed by tenantId for context-aware queries.</li>
 * </ul>
 */
@Serdeable
@Introspected
@MappedEntity("users")
@Indexes({
        // Enforces corporate email uniqueness within the same organizational context
        @Index(columns = {"tenantId", "email"}, unique = true),
        // Enforces system-wide unique login identification
        @Index(columns = {"tenantId", "username"}, unique = true),
        // Optimizes administrative lookups and operational status filtering
        @Index(columns = {"tenantId", "status"}),
        @Index(columns = {"parentId"})
})
public record UserEntity(
        @Id
        @Nonnull
        UUID id,

        @Nonnull
        UUID tenantId,

        @Nullable
        UUID parentId,

        @Nonnull
        String username,

        @Nonnull
        String email,

        @Nonnull
        UserStatus status,

        @Nonnull
        UserLevel level,

        @Nonnull
        UserProfile profile,

        @Nonnull
        List<String> roles,

        @Nonnull
        Integer failedAttempts,

        @Nonnull
        Boolean mfaEnabled,

        @Nonnull
        String createdBy,

        @Nonnull
        Instant createdAt,

        @Nullable
        String updatedBy,

        @Nullable
        Instant updatedAt,

        @Version
        @Nonnull
        Long version
) {

    /**
     * Factory: Maps a Pure Domain Aggregate to an Infrastructure Entity.
     * Guaranteed to preserve identity integrity and versioning context.
     */
    public static UserEntity fromDomain(@Nonnull User domain) {
        return new UserEntity(
                domain.id(),
                domain.tenantId(),
                domain.parentId(),
                domain.username(),
                domain.profile().corporateEmail(), // Redundant for direct indexing performance
                domain.status(),
                domain.level(),
                domain.profile(),
                List.copyOf(domain.roles()),
                domain.failedAttempts(),
                domain.mfaEnabled(),
                domain.createdBy(),
                domain.createdAt(),
                domain.updatedBy(),
                domain.updatedAt(),
                domain.version()
        );
    }

    /**
     * Transformation: Projects the Infrastructure Entity back to a Pure Domain Model.
     * Restores business capability while stripping database-specific concerns.
     */
    public User toDomain() {
        return new User(
                this.id,
                this.tenantId,
                this.parentId,
                this.username,
                this.level,
                this.status,
                this.profile,
                Collections.unmodifiableList(this.roles),
                this.failedAttempts,
                this.mfaEnabled,
                this.createdBy,
                this.createdAt,
                this.updatedBy,
                this.updatedAt,
                this.version != null ? this.version : 0L
        );
    }
}