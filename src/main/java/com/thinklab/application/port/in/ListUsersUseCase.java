package com.thinklab.application.port.in;

import com.thinklab.application.usecase.query.ListUsersQuery;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

/**
 * Application Port: Input boundary for listing and searching Enterprise Identities (Users).
 * Following the CQRS principle, this interface represents a pure read-only operation
 * designed for high-performance data egress and reactive streaming.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the core
 * application logic from inbound adapters like REST Controllers or Reporting Engines.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Backpressure-Aware:</b> Strictly utilizes Flux to support non-blocking
 *         stream processing of identity datasets.</li>
 *     <li><b>Multi-tenant Sovereignty:</b> Enforces tenant-based scoping during
 *         discovery to prevent cross-tenant data leakage.</li>
 *     <li><b>Resource Protection:</b> Operates under strict pagination boundaries
 *         to maintain system stability under high load.</li>
 * </ul>
 */
public interface ListUsersUseCase {

    /**
     * Executes the paginated discovery of user identities based on the provided
     * search criteria and tenant context.
     *
     * @param query The {@link ListUsersQuery} encapsulating tenant isolation,
     *              lifecycle filters, and pagination metadata.
     * @return A {@link Flux} streaming the matching {@link User} aggregates.
     * @throws NullPointerException if the provided query is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote In case of no results, the Flux will complete normally without
     *          emitting items, adhering to reactive stream conventions.
     */
    @Nonnull
    Flux<User> execute(@Nonnull ListUsersQuery query);
}