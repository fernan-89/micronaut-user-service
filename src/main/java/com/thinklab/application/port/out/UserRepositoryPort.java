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
 */
public interface UserRepositoryPort {

    @Nonnull
    Mono<User> save(@Nonnull User user);

    @Nonnull
    Mono<User> update(@Nonnull User user);

    @Nonnull
    Mono<User> findById(@Nonnull UUID id);

    /**
     * Retrieves a user by ID and TenantID to ensure multi-tenant isolation.
     */
    @Nonnull
    Mono<User> findByIdAndTenantId(@Nonnull UUID id, @Nonnull UUID tenantId);

    @Nonnull
    Mono<Boolean> existsByTenantIdAndUsername(@Nonnull UUID tenantId, @Nonnull String username);

    @Nonnull
    Mono<Boolean> existsByTenantIdAndEmail(@Nonnull UUID tenantId, @Nonnull String email);

    @Nonnull
    Flux<User> findByTenantId(@Nonnull UUID tenantId, @Nonnull Pageable pageable);

    @Nonnull
    Flux<User> findByTenantIdAndStatus(@Nonnull UUID tenantId, @Nonnull UserStatus status, @Nonnull Pageable pageable);
}