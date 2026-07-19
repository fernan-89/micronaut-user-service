package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactiveMongoRepository;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Infrastructure Repository: Reactive persistence interface for {@link UserEntity}.
 * This interface leverages Micronaut Data's AOT compilation to generate high-performance,
 * non-blocking MongoDB driver calls. It handles all CRUD operations reactively,
 * ensuring optimal resource utilization under high-concurrency identity traffic.
 *
 * <p><b>Architectural Constraints (NASA Standard):</b></p>
 * <ul>
 *     <li><b>BSON Binary UUIDs:</b> Uses native UUIDs for optimized indexing and storage.</li>
 *     <li><b>Zero Reflection:</b> Implementation is generated at compile-time for GraalVM support.</li>
 *     <li><b>Multi-tenant Partitioning:</b> All queries are strictly scoped by tenantId.</li>
 * </ul>
 */
@MongoRepository
public interface MongoUserRepository extends ReactiveMongoRepository<UserEntity, UUID> {

    /**
     * Enforces business uniqueness by checking email presence within a tenant context.
     *
     * @param tenantId The isolated organizational context.
     * @param email    The corporate email to verify.
     * @return A {@link Mono} emitting true if the identity exists, otherwise false.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndEmail(@Nonnull UUID tenantId, @Nonnull String email);

    /**
     * Retrieves a paginated stream of identities scoped to a specific organization.
     *
     * @param tenantId The isolated organizational context.
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Flux} streaming matching {@link UserEntity} records.
     */
    @Nonnull
    Flux<UserEntity> findByTenantId(@Nonnull UUID tenantId, @Nonnull Pageable pageable);

    /**
     * Retrieves a paginated stream of identities filtered by tenant and operational status.
     *
     * @param tenantId The isolated organizational context.
     * @param status   The target {@link UserStatus} (e.g., ACTIVE, BLOCKED).
     * @param pageable Pagination metadata.
     * @return A {@link Flux} streaming the filtered entities.
     */
    @Nonnull
    Flux<UserEntity> findByTenantIdAndStatus(@Nonnull UUID tenantId, @Nonnull UserStatus status, @Nonnull Pageable pageable);
}