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
 */
@Serdeable
@Introspected
@MappedEntity("users")
@Indexes({
        @Index(columns = {"tenantId", "email"}, unique = true),
        @Index(columns = {"tenantId", "username"}, unique = true),
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

    public static UserEntity fromDomain(@Nonnull User domain) {
        return new UserEntity(
                domain.id(),
                domain.tenantId(),
                domain.parentId(),
                domain.username(),
                domain.profile().corporateEmail(),
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

    public User toDomain() {
        return new User(
                this.id,
                this.tenantId,
                this.parentId,
                this.username,
                this.level,     // Alinhado com o record User
                this.status,    // Alinhado com o record User
                this.roles != null ? Collections.unmodifiableList(this.roles) : Collections.emptyList(),
                this.profile,
                this.failedAttempts,
                this.mfaEnabled,
                this.createdBy,
                this.createdAt,
                this.updatedBy,
                this.updatedAt,
                this.version
        );
    }
}