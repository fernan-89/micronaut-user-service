package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import com.thinklab.infrastructure.adapter.out.mongo.repository.MongoUserRepository;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistence Adapter: Implementation of the {@link UserRepositoryPort} for MongoDB.
 * This class acts as a high-fidelity bridge between the Core Domain and the
 * physical storage layer, enforcing reactive non-blocking execution and
 * strict mutational boundaries.
 *
 * <p><b>Architectural Principles (NASA Standard):</b></p>
 * <ul>
 *     <li><b>Mutational Integrity:</b> Separates save (insert) from update (replace)
 *         to prevent E11000 primary key collisions.</li>
 *     <li><b>Anti-Corruption Layer:</b> Encapsulates mapping between Domain Records
 *         and Infrastructure Entities.</li>
 *     <li><b>Reactive Egress:</b> Maintains zero-blocking I/O throughout the
 *         persistence pipeline.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final MongoUserRepository repository;

    /**
     * Explicit constructor injection for Micronaut AOT resilience.
     */
    @Inject
    public UserRepositoryAdapter(MongoUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Nonnull
    public Mono<User> save(@Nonnull User user) {
        Objects.requireNonNull(user, "User aggregate cannot be null");

        return repository.save(UserEntity.fromDomain(user))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalStateException(
                        "Database failed to return the saved entity. Check for insert vs update conflicts."))))
                .map(UserEntity::toDomain)
                .doOnSuccess(u -> log.info("[ACTION: PERSIST_USER] [ID: {}] - Identity provisioned successfully.", u.id()))
                .doOnError(e -> log.error("[ACTION: PERSIST_USER] [ID: {}] - Persistence failure: {}", user.id(), e.getMessage()));
    }

    @Override
    @Nonnull
    public Mono<User> update(@Nonnull User user) {
        Objects.requireNonNull(user, "User aggregate cannot be null");

        return repository.update(UserEntity.fromDomain(user))
                .map(UserEntity::toDomain)
                .doOnSuccess(u -> log.debug("[ACTION: SYNCHRONIZE_USER] [ID: {}] - State mutation committed.", u.id()))
                .doOnError(e -> log.error("[ACTION: SYNCHRONIZE_USER] [ID: {}] - Mutation failed: {}", user.id(), e.getMessage()));
    }

    @Override
    @Nonnull
    public Mono<User> findById(@Nonnull UUID id) {
        return repository.findById(id)
                .map(UserEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_USER_BY_ID] [ID: {}] - Retrieval error: {}", id, e.getMessage()));
    }

    @Override
    @Nonnull
    public Mono<Boolean> existsByTenantIdAndEmail(@Nonnull UUID tenantId, @Nonnull String email) {
        return repository.existsByTenantIdAndEmail(tenantId, email)
                .defaultIfEmpty(false); // Reactive safety: Ensure signal emission even on empty results
    }

    @Override
    @Nonnull
    public Flux<User> findByTenantId(@Nonnull UUID tenantId, @Nonnull Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable)
                .map(UserEntity::toDomain);
    }

    @Override
    @Nonnull
    public Flux<User> findByTenantIdAndStatus(@Nonnull UUID tenantId, @Nonnull UserStatus status, @Nonnull Pageable pageable) {
        return repository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(UserEntity::toDomain);
    }
}