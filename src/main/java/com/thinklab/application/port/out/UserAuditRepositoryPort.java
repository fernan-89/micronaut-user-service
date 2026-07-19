package com.thinklab.application.port.out;

import com.thinklab.domain.model.UserAudit;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application Output Port: Repository contract for the {@link UserAudit} aggregate persistence.
 * This interface defines the mandatory storage behavior for forensic audit records
 * within the IAM ecosystem. Designed for high-assurance environments, it ensures
 * that security-critical events are captured as an immutable, irrefutable timeline.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Append-Only Integrity:</b> Modification and deletion methods are strictly
 *         excluded to prevent historical tampering (SOC2/Zero Trust compliance).</li>
 *     <li><b>Reactive Sovereignty:</b> Strictly utilizes Mono and Flux to maintain
 *         non-blocking execution during high-throughput audit bursts.</li>
 *     <li><b>Forensic Isolation:</b> Enforces tenant-based partitioning for all
 *         historical state reconstruction queries.</li>
 * </ul>
 */
public interface UserAuditRepositoryPort {

    /**
     * Persists a new immutable forensic audit record.
     *
     * @param audit The validated {@link UserAudit} entry to be stored.
     * @return A {@link Mono} emitting the successfully persisted audit instance.
     * @throws NullPointerException if the provided audit object is null.
     */
    @Nonnull
    Mono<UserAudit> save(@Nonnull UserAudit audit);

    /**
     * Retrieves a specific audit record by its unique system identifier.
     *
     * @param id The unique UUID of the audit entry.
     * @return A {@link Mono} emitting the found record, or an empty signal if non-existent.
     */
    @Nonnull
    Mono<UserAudit> findById(@Nonnull UUID id);

    /**
     * Retrieves the complete historical timeline for a specific user.
     *
     * @param tenantId The isolated organizational context.
     * @param userId   The unique identifier of the target user entity.
     * @return A {@link Flux} streaming the chronological audit trail for the user.
     */
    @Nonnull
    Flux<UserAudit> findByUserId(@Nonnull UUID tenantId, @Nonnull UUID userId);

    /**
     * Retrieves all forensic events associated with a specific transaction.
     * Useful for cross-service correlation and troubleshooting.
     *
     * @param txId The correlation transaction identifier.
     * @return A {@link Flux} streaming related audit entries.
     */
    @Nonnull
    Flux<UserAudit> findByTxId(@Nonnull String txId);

    /**
     * Retrieves all audit records scoped to a specific tenant for compliance reporting.
     *
     * @param tenantId The isolated organizational context.
     * @return A {@link Flux} streaming the organization's forensic history.
     */
    @Nonnull
    Flux<UserAudit> findByTenantId(@Nonnull UUID tenantId);
}