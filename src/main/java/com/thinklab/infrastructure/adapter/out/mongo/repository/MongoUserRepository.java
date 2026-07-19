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
 *
 * <p><b>Architectural Constraints:</b></p>
 * <ul>
 *     <li><b>BSON Binary UUIDs:</b> Uses native UUIDs for optimized indexing.</li>
 *     <li><b>Zero Reflection:</b> Implementation generated at compile-time for GraalVM support.</li>
 *     <li><b>Multi-tenant Partitioning:</b> All queries are strictly scoped by tenantId.</li>
 * </ul>
 */
@MongoRepository
public interface MongoUserRepository extends ReactorPageableRepository<UserEntity, UUID> {

    /**
     * Verifies identity uniqueness within a tenant boundary.
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