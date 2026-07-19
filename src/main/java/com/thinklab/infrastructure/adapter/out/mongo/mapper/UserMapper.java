package com.thinklab.infrastructure.adapter.out.mongo.mapper;

import com.thinklab.domain.model.User;
import com.thinklab.infrastructure.adapter.in.web.dto.response.UserResponse;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Infrastructure Mapper: Centralized Anti-Corruption Layer (ACL) for the User domain.
 */
@Singleton
public class UserMapper {

    /**
     * Projects a Domain Aggregate into a Persistence Entity.
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
                entity.roles(),
                entity.profile(),
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
                domain.roles(),
                domain.mfaEnabled(),
                domain.createdBy(),
                domain.createdAt(),
                domain.updatedBy(),
                domain.updatedAt(),
                domain.version()
        );
    }
}