package com.thinklab.application.port.out;

import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application Output Port: Repository contract for the {@link User} aggregate persistence.
 * This interface defines the mandatory storage behavior for Enterprise Identities,
 * ensuring that the Core Domain remains strictly decoupled from infrastructure
 * frameworks or specific database drivers (e.g., MongoDB, PostgreSQL).
 *
 * <p><b>Architectural Constraints (ADR 001/002):</b></p>
 * <ul>
 *     <li><b>Domain Sovereignty:</b> Accepts and returns only {@link User} domain aggregates;
 *         mapping to persistence entities is the responsibility of the infrastructure adapters.</li>
 *     <li><b>Mutational Integrity:</b> Distinguished methods for creation vs. update
 *         to prevent primary ID collisions and ensure version control.</li>
 *     <li><b>Reactive Flow:</b> Strictly utilizes Mono and Flux to maintain
 *         non-blocking I/O across the entire pipeline.</li>
 *     <li><b>Data Isolation:</b> Queries are engineered to support multi-tenant
 *         partitioning via tenantId enforcement.</li>
 * </ul>
 */
public interface UserRepositoryPort {

    /**
     * Persists the initial state of a new User identity.
     *
     * @param user The validated {@link User} aggregate to be provisioned.
     * @return A {@link Mono} emitting the successfully persisted User instance.
     * @throws NullPointerException if the provided aggregate is null.
     */
    @Nonnull
    Mono<User> save(@Nonnull User user);

    /**
     * Commits state mutations for an existing identity (Status, Profile, or Roles).
     * This method enforces atomic update semantics to protect against concurrency conflicts.
     *
     * @param user The mutated aggregate root carrying the updated state and version.
     * @return A {@link Mono} emitting the synchronized {@link User} state.
     */
    @Nonnull
    Mono<User> update(@Nonnull User user);

    /**
     * Retrieves a specific user by its unique system identifier.
     *
     * @param id The native UUID of the target identity.
     * @return A {@link Mono} emitting the found User, or an empty signal if non-existent.
     */
    @Nonnull
    Mono<User> findById(@Nonnull UUID id);

    /**
     * Validates identity uniqueness within a specific organizational context.
     *
     * @param tenantId The isolated tenant context (Holding or Branch).
     * @param email    The corporate email to be checked.
     * @return A {@link Mono} emitting true if a conflict exists, false otherwise.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndEmail(@Nonnull UUID tenantId, @Nonnull String email);

    /**
     * Retrieves a paginated stream of identities scoped to a specific tenant.
     *
     * @param tenantId The isolated organizational context.
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Flux} streaming matching {@link User} aggregates.
     */
    @Nonnull
    Flux<User> findByTenantId(@Nonnull UUID tenantId, @Nonnull Pageable pageable);

    /**
     * Retrieves a paginated stream of identities filtered by organization and lifecycle status.
     *
     * @param tenantId The isolated organizational context.
     * @param status   The target {@link UserStatus} filter (e.g., ACTIVE, BLOCKED).
     * @param pageable Pagination metadata.
     * @return A {@link Flux} streaming the matching identities.
     */
    @Nonnull
    Flux<User> findByTenantIdAndStatus(@Nonnull UUID tenantId, @Nonnull UserStatus status, @Nonnull Pageable pageable);
}