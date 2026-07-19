package com.thinklab.infrastructure.adapter.out.mongo.mapper;

import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.infrastructure.adapter.in.web.dto.response.UserResponse;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

/**
 * Infrastructure Mapper: Centralized Anti-Corruption Layer (ACL) for the User domain.
 * This component orchestrates high-fidelity transformations between Domain Aggregates,
 * Persistence Entities, and public Response DTOs. By centralizing mapping logic,
 * it ensures that framework-specific concerns (like BSON UUIDs) do not pollute
 * the core business models.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Layer Isolation:</b> Shields the Domain from database and transport schemas.</li>
 *     <li><b>Type Safety:</b> Manages strict conversions between String IDs and native UUIDs.</li>
 *     <li><b>Consistency:</b> Guarantees a single point of truth for object projections.</li>
 * </ul>
 */
@Singleton
public class UserMapper {

    /**
     * Projects a Domain Aggregate into a Persistence Entity.
     * Enforces the conversion to native BSON Binary UUIDs for database performance.
     */
    @Nonnull
    public UserEntity toEntity(@Nonnull User domain) {
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

    /**
     * Restores a Domain Aggregate from a Persistence Entity.
     * Reconstitutes the rich business model from the flattened infrastructure record.
     */
    @Nonnull
    public User toDomain(@Nonnull UserEntity entity) {
        return new User(
                entity.id(),
                entity.tenantId(),
                entity.parentId(),
                entity.username(),
                entity.level(),
                entity.status(),
                entity.profile(),
                List.copyOf(entity.roles()),
                entity.failedAttempts(),
                entity.mfaEnabled(),
                entity.createdBy(),
                entity.createdAt(),
                entity.updatedBy(),
                entity.updatedAt(),
                entity.version()
        );
    }

    /**
     * Transforms a Domain Aggregate into a sanitized Public Response.
     * Implements the Projection Pattern to prevent internal state leakage.
     */
    @Nonnull
    public UserResponse toResponse(@Nonnull User domain) {
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