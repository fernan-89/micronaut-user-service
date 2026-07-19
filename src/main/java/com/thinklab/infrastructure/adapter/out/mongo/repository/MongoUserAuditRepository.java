package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.infrastructure.adapter.out.mongo.entity.UserAuditEntity;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactiveMongoRepository;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Infrastructure Repository: Reactive persistence interface for {@link UserAuditEntity}.
 * This repository governs the append-only storage of forensic audit records within
 * the IAM ecosystem. It leverages Micronaut Data's AOT engine to generate
 * reflection-free, non-blocking MongoDB driver implementations.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>BSON Binary UUIDs:</b> Native UUID mapping for high-performance indexing.</li>
 *     <li><b>Forensic Isolation:</b> Strict tenant-based partitioning for all history queries.</li>
 *     <li><b>Reactive Egress:</b> Utilizes Flux for backpressure-aware audit streaming.</li>
 * </ul>
 */
@MongoRepository
public interface MongoUserAuditRepository extends ReactiveMongoRepository<UserAuditEntity, UUID> {

    /**
     * Retrieves the complete historical timeline for a specific user within a tenant.
     * Results are ordered by timestamp descending to provide the latest events first.
     *
     * @param tenantId The isolated organizational context.
     * @param userId   The unique identifier of the target identity.
     * @return A {@link Flux} streaming the matching audit records.
     */
    @Nonnull
    Flux<UserAuditEntity> findByTenantIdAndUserIdOrderByTimestampDesc(@Nonnull UUID tenantId, @Nonnull UUID userId);

    /**
     * Retrieves all forensic events associated with a specific transaction.
     *
     * @param txId The unique correlation transaction identifier.
     * @return A {@link Flux} streaming related audit entries.
     */
    @Nonnull
    Flux<UserAuditEntity> findByTxId(@Nonnull String txId);

    /**
     * Retrieves the chronological audit trail for an entire organization.
     *
     * @param tenantId The isolated organizational context.
     * @return A {@link Flux} streaming the organization's forensic history.
     */
    @Nonnull
    Flux<UserAuditEntity> findByTenantId(@Nonnull UUID tenantId);
}