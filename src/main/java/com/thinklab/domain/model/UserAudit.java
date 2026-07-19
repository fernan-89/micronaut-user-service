package com.thinklab.domain.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model: Immutable aggregate representing a forensic audit entry for User operations.
 * This model serves as the source of truth for all security-critical operations within the
 * IAM ecosystem, guaranteeing the integrity of the audit trail through strictly immutable state.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Immutability:</b> Ensures audit history cannot be altered post-persistence,
 *         satisfying strict forensic requirements.</li>
 *     <li><b>Append-Only:</b> Designed for insertion-only flows, preventing historical corruption.</li>
 *     <li><b>Deterministic Correlation:</b> Uses transaction IDs to link complex multi-service operations.</li>
 * </ul>
 *
 * @param id         The unique system identifier for this audit record.
 * @param txId       The correlation identifier for the encompassing transaction.
 * @param tenantId   The isolated tenant context (Holding or Branch) where the event occurred.
 * @param userId     The identifier of the user entity affected by the operation.
 * @param operation  The business operation type (e.g., USER_PROVISIONING, STATUS_CHANGE).
 * @param status     The resulting outcome of the operation (e.g., SUCCESS, FAILURE).
 * @param executorId The identity of the agent (system or admin) authorizing the action.
 * @param timestamp  The UTC instant when the event was recorded.
 * @param metadata   Contextual data providing additional details for forensic analysis.
 */
public record UserAudit(
        @Nonnull UUID id,
        @Nonnull String txId,
        @Nonnull UUID tenantId,
        @Nonnull UUID userId,
        @Nonnull String operation,
        @Nonnull String status,
        @Nonnull String executorId,
        @Nonnull Instant timestamp,
        @Nonnull Map<String, Object> metadata
) {

    /**
     * Compact constructor for defensive programming and invariant protection.
     * Enforces that all mandatory audit fields are present before instantiation.
     */
    public UserAudit {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(txId, "txId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(executorId, "executorId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");

        // Defensive copy of metadata to ensure true immutability
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    /**
     * Factory Method: Standardized creation of a new User Audit record.
     * Generates internal identifiers and timestamps automatically to ensure consistency.
     *
     * @param tenantId   Organization identifier.
     * @param userId     Affected user identifier.
     * @param operation  Business action performed.
     * @param status     Outcome of the operation.
     * @param executorId Authorizing agent.
     * @param metadata   Additional key-value pairs for forensic context.
     * @return A fully initialized, immutable {@link UserAudit} instance.
     */
    public static UserAudit create(
            @Nonnull UUID tenantId,
            @Nonnull UUID userId,
            @Nonnull String operation,
            @Nonnull String status,
            @Nonnull String executorId,
            @Nullable Map<String, Object> metadata
    ) {
        return new UserAudit(
                UUID.randomUUID(),
                UUID.randomUUID().toString(), // Future: replace with traceId from context
                tenantId,
                userId,
                operation,
                status,
                executorId,
                Instant.now(),
                metadata
        );
    }
}