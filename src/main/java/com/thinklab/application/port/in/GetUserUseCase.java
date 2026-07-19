package com.thinklab.application.port.in;

import com.thinklab.application.usecase.query.GetUserQuery;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for retrieving a specific Enterprise Identity (User).
 * Following the CQRS principle, this interface represents a pure read-only operation
 * designed to safely expose user data within strict multi-tenant isolation boundaries.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the core
 * application logic from inbound adapters like REST Controllers or GraphQL Resolvers.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>CQRS Compliance:</b> Strictly separates data retrieval logic from
 *         state mutation commands.</li>
 *     <li><b>Multi-tenant Sovereignty:</b> Mandates tenant-based scoping during
 *         retrieval to prevent unauthorized cross-tenant access.</li>
 *     <li><b>Reactive Performance:</b> Strictly utilizes Project Reactor types to
 *         maintain non-blocking execution across the IAM cluster.</li>
 * </ul>
 */
public interface GetUserUseCase {

    /**
     * Executes the secure retrieval of a user identity based on the provided query criteria.
     *
     * @param query The {@link GetUserQuery} encapsulating the target unique identifier
     *              and the mandatory tenant context for security enforcement.
     * @return A {@link Mono} emitting the requested {@link User} aggregate state.
     * @throws NullPointerException if the provided query is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@code UserNotFoundException} signal through the reactive
     *          pipeline if the identity does not exist within the specified tenant context.
     */
    @Nonnull
    Mono<User> execute(@Nonnull GetUserQuery query);
}