package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.domain.model.UserAudit;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserAuditEntity;
import com.thinklab.infrastructure.adapter.out.mongo.repository.MongoUserAuditRepository;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistence Adapter: Implementation of the {@link UserAuditRepositoryPort} for MongoDB.
 * This class serves as an Anti-Corruption Layer (ACL), managing the immutable storage
 * of forensic events. It enforces the "Append-Only" principle required for security
 * compliance and high-assurance environments.
 *
 * <p><b>Architectural Constraints:</b></p>
 * <ul>
 *     <li><b>Immutable History:</b> Strictly insertion-based; update and delete operations are prohibited.</li>
 *     <li><b>Reactive Egress:</b> Maintains zero-blocking execution using Project Reactor.</li>
 *     <li><b>AOT-Compliant DI:</b> Uses explicit constructor injection for Micronaut optimization.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class UserAuditRepositoryAdapter implements UserAuditRepositoryPort {

    private final MongoUserAuditRepository repository;

    /**
     * Explicit constructor injection to ensure AOT resilience and proxy clarity.
     */
    @Inject
    public UserAuditRepositoryAdapter(MongoUserAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Nonnull
    public Mono<UserAudit> save(@Nonnull UserAudit audit) {
        Objects.requireNonNull(audit, "UserAudit aggregate cannot be null");

        return repository.save(UserAuditEntity.fromDomain(audit))
                .map(UserAuditEntity::toDomain)
                .doOnSuccess(a -> log.debug("[ACTION: PERSIST_AUDIT] [TX: {}] - Forensic record committed.", a.txId()))
                .doOnError(e -> log.error("[ACTION: PERSIST_AUDIT] [USER: {}] - Persistence failure: {}",
                        audit.userId(), e.getMessage()));
    }

    @Override
    @Nonnull
    public Mono<UserAudit> findById(@Nonnull UUID id) {
        return repository.findById(id)
                .map(UserAuditEntity::toDomain);
    }

    @Override
    @Nonnull
    public Flux<UserAudit> findByUserId(@Nonnull UUID tenantId, @Nonnull UUID userId) {
        return repository.findByTenantIdAndUserIdOrderByTimestampDesc(tenantId, userId)
                .map(UserAuditEntity::toDomain);
    }

    @Override
    @Nonnull
    public Flux<UserAudit> findByTxId(@Nonnull String txId) {
        Objects.requireNonNull(txId, "Transaction ID cannot be null");
        return repository.findByTxId(txId)
                .map(UserAuditEntity::toDomain);
    }

    @Override
    @Nonnull
    public Flux<UserAudit> findByTenantId(@Nonnull UUID tenantId) {
        return repository.findByTenantId(tenantId)
                .map(UserAuditEntity::toDomain);
    }
}