package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Infrastructure DTO: Public-facing projection of an Enterprise Identity (User).
 * This record follows the Projection Pattern to transform the internal Domain Aggregate
 * into a sanitized representation, supporting global multi-tenancy and hierarchical
 * unit associations. It acts as a security boundary to prevent internal state leakage.
 */
@Serdeable
@Introspected
@Schema(
        name = "UserResponse",
        description = "Standardized response payload representing an identity's state and its global localization metadata."
)
public record UserResponse(
        @Nonnull
        @Schema(description = "Unique system identifier (UUID)", example = "7c6b0035-2033-4ac5-9658-9060fd1f7a1e")
        UUID id,

        @Nonnull
        @Schema(description = "Isolated organizational context identifier", example = "af9c2b03-384d-3a6d-8465-64ba383d3bad")
        UUID tenantId,

        @Nullable
        @Schema(description = "Optional identifier of the parent business unit", example = "79e83370-d699-4d28-817f-acc401490ed5")
        UUID parentId,

        @Nonnull
        @Schema(description = "Unique system login name", example = "john.doe")
        String username,

        @Nonnull
        @Schema(description = "Current lifecycle status", example = "ACTIVE")
        UserStatus status,

        @Nonnull
        @Schema(description = "Hierarchical level defining global authority", example = "OPERATOR")
        UserLevel level,

        @Nonnull
        @Schema(description = "Consolidated biographical and localization metadata")
        UserProfile profile,

        @Nonnull
        @Schema(description = "Collection of assigned functional roles", example = "[\"REPORTS_VIEWER\"]")
        List<String> roles,

        @Nonnull
        @Schema(description = "Identity was provisioned with MFA protection active")
        Boolean mfaEnabled,

        @Nonnull
        @Schema(description = "Identifier of the agent who provisioned this identity")
        String createdBy,

        @Nonnull
        @Schema(description = "UTC timestamp of initial provisioning")
        Instant createdAt,

        @Nullable
        @Schema(description = "Identifier of the last agent to mutate this identity")
        String updatedBy,

        @Nullable
        @Schema(description = "UTC timestamp of the last administrative mutation")
        Instant updatedAt,

        @Nonnull
        @Schema(description = "Concurrency control version for consistency tracking", example = "1")
        Long version
) {

    /**
     * Factory: Maps a Pure Domain Aggregate to a sanitized Infrastructure Response.
     * Restores boundary security by projecting only public-safe fields.
     */
    public static UserResponse fromDomain(@Nonnull User domain) {
        return new UserResponse(
                domain.id(),
                domain.tenantId(),
                domain.parentId(),
                domain.username(),
                domain.status(),
                domain.level(),
                domain.profile(),
                List.copyOf(domain.roles()),
                domain.mfaEnabled(),
                domain.createdBy(),
                domain.createdAt(),
                domain.updatedBy(),
                domain.updatedAt(),
                domain.version()
        );
    }
}