package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorPageableRepository;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Infrastructure Repository: Reactive persistence interface for {@link UserEntity}.
 * This interface leverages Micronaut Data's AOT compilation to generate high-performance,
 * non-blocking MongoDB driver calls.
 */
@MongoRepository
public interface MongoUserRepository extends ReactorPageableRepository<UserEntity, UUID> {

    /**
     * Retrieves a user by ID and TenantID to ensure multi-tenant isolation.
     */
    @Nonnull
    Mono<UserEntity> findByIdAndTenantId(@Nonnull UUID id, @Nonnull UUID tenantId);

    /**
     * Verifies identity uniqueness (Username) within a tenant boundary.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndUsername(@Nonnull UUID tenantId, @Nonnull String username);

    /**
     * Verifies identity uniqueness (Email) within a tenant boundary.
     * Uses composite index on (tenantId, email) for O(log n) lookup.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndEmail(@Nonnull UUID tenantId, @Nonnull String email);

    /**
     * Fetches all users for a given tenant, supporting paginated navigation.
     */
    @Nonnull
    Flux<UserEntity> findByTenantId(@Nonnull UUID tenantId, @Nonnull Pageable pageable);

    /**
     * Filters users by tenant and operational lifecycle status.
     */
    @Nonnull
    Flux<UserEntity> findByTenantIdAndStatus(@Nonnull UUID tenantId,
                                             @Nonnull UserStatus status,
                                             @Nonnull Pageable pageable);
}