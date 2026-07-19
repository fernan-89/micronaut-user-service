package com.thinklab.application.usecase.query;

import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Query: Encapsulates the intent to search and list Enterprise Identities
 * with support for pagination, status filtering, and multi-tenant isolation.
 *
 * <p>Following the CQRS principle, this query represents a read-only operation
 * designed for high-performance retrieval while enforcing strict boundary limits
 * to prevent resource exhaustion.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Multi-tenant Sovereignty:</b> Mandatory tenantId ensures that query
 *         results are strictly confined to the organization's context.</li>
 *     <li><b>Resource Protection:</b> Enforces strict limits on page size to
 *         mitigate heap exhaustion and database overhead.</li>
 *     <li><b>Syntactic Sanitization:</b> Normalizes pagination parameters and
 *         validates filter tiers at the entry boundary.</li>
 * </ul>
 *
 * @param tenantId The isolated organizational context (Holding or Branch).
 * @param status   Optional lifecycle status filter (e.g., ACTIVE, BLOCKED).
 * @param level    Optional authority tier filter (e.g., OPERATOR).
 * @param page     Zero-based index of the target page (Default: 0).
 * @param size     Maximum records per result set (Default: 20, Max: 100).
 */
@Slf4j
@Introspected
public record ListUsersQuery(
        @NotNull(message = "Tenant context is mandatory for search isolation")
        UUID tenantId,

        @Nullable
        UserStatus status,

        @Nullable
        UserLevel level,

        @Min(value = 0, message = "Page index cannot be negative")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size exceeds the safety limit of 100")
        Integer size
) {

    /**
     * Compact constructor for defensive programming and input sanitization.
     * Guarantees a well-formed query object with safe default values.
     */
    public ListUsersQuery {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        // NASA Standard: Apply safe defaults for pagination if not provided
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;

        // Read Audit logging at the application boundary
        log.info("[ACTION: LIST_USERS_QUERY] [TENANT: {}] [STATUS: {}] [LEVEL: {}] " +
                        "[PAGE: {}] [SIZE: {}] - Initiating secure discovery intent.",
                tenantId,
                (status != null ? status : "ALL"),
                (level != null ? level : "ALL"),
                page,
                size);
    }
}