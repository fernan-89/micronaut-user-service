package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.UserAudit;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.Indexes;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure Entity: Persistence model for the forensic audit trail (Append-Only).
 * This record maps the {@link UserAudit} domain model to the MongoDB "user_audits"
 * collection. Designed for high-assurance environments, it ensures that every
 * identity mutation is recorded as an immutable, irrefutable event.
 *
 * <p><b>Persistence Principles (Mission-Critical):</b></p>
 * <ul>
 *     <li><b>Append-Only Integrity:</b> This entity is never updated or deleted.</li>
 *     <li><b>BSON Binary UUIDs:</b> Uses native UUID types for optimized indexing.</li>
 *     <li><b>Multi-tenant Scoping:</b> Strictly indexed by tenantId for audit isolation.</li>
 * </ul>
 */
@Serdeable
@Introspected
@MappedEntity("user_audits")
@Indexes({
        // Optimizes historical timeline lookups for a specific identity within a tenant
        @Index(columns = {"tenantId", "userId", "timestamp"}),
        // Allows forensic investigation by transaction correlation identifier
        @Index(columns = {"txId"}),
        // Supports chronological compliance reporting at the organization level
        @Index(columns = {"tenantId", "timestamp"}),
        @Index(columns = {"operation"})
})
public record UserAuditEntity(
        @Id
        @Nonnull
        UUID id,

        @Nonnull
        String txId,

        @Nonnull
        UUID tenantId,

        @Nonnull
        UUID userId,

        @Nonnull
        String operation,

        @Nonnull
        String status,

        @Nonnull
        String executorId,

        @Nonnull
        Instant timestamp,

        @Nonnull
        Map<String, Object> metadata
) {

    /**
     * Factory: Maps a Pure Domain Audit model to its Infrastructure counterpart.
     * Enforces strict conversion of ID strings to native UUIDs for BSON storage.
     */
    public static UserAuditEntity fromDomain(@Nonnull UserAudit domain) {
        return new UserAuditEntity(
                UUID.fromString(domain.id()),
                domain.txId(),
                UUID.fromString(domain.tenantId()),
                UUID.fromString(domain.userId()),
                domain.operation(),
                domain.status(),
                domain.executorId(),
                domain.timestamp(),
                Map.copyOf(domain.metadata())
        );
    }

    /**
     * Transformation: Restores the Domain Model from the Infrastructure representation.
     * Projects the BSON data back into a pure, framework-agnostic record.
     */
    public UserAudit toDomain() {
        return new UserAudit(
                this.id.toString(),
                this.txId,
                this.tenantId.toString(),
                this.userId.toString(),
                this.operation,
                this.status,
                this.executorId,
                this.timestamp,
                this.metadata
        );
    }
}