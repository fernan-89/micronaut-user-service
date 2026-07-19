package com.thinklab.application.usecase.query;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Query: Encapsulates the intent to retrieve a specific Enterprise Identity.
 * Following the CQRS principle, this query represents a pure read-only operation,
 * decoupled from command-based state mutations.
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Data Isolation:</b> Mandatory inclusion of tenantId to enforce strict
 *         multi-tenant boundaries during retrieval.</li>
 *     <li><b>Syntactic Security:</b> Uses native UUIDs to prevent malformed
 *         identifiers from reaching the persistence layer.</li>
 *     <li><b>Read-Side Observability:</b> Logs the retrieval attempt at the boundary
 *         for access auditing.</li>
 * </ul>
 *
 * @param userId   The unique system identifier (UUID) of the target user.
 * @param tenantId The isolated organizational context (Holding or Branch)
 *                 mandatory for security enforcement.
 */
@Slf4j
@Introspected
public record GetUserQuery(
        @NotNull(message = "User ID is mandatory for retrieval")
        UUID userId,

        @NotNull(message = "Tenant context is mandatory for security isolation")
        UUID tenantId
) {

    /**
     * Compact constructor for defensive programming and access observability.
     * Guarantees that only well-formed queries enter the application interactors.
     */
    public GetUserQuery {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        // NASA Standard: Read Audit logging at the application boundary
        log.info("[ACTION: GET_USER_QUERY] [ID: {}] [TENANT: {}] - Initializing secure retrieval intent.",
                userId, tenantId);
    }
}