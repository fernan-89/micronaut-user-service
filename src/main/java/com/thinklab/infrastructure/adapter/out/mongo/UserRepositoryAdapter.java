package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.out.mongo.mapper.UserMapper;
import com.thinklab.infrastructure.adapter.out.mongo.repository.MongoUserRepository;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Infrastructure Adapter: Implementation of {@link UserRepositoryPort}.
 * Encapsulates the mapping between Domain aggregates and MongoDB entities.
 */
@Singleton
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final MongoUserRepository repository;
    private final UserMapper mapper;

    @Override
    public Mono<User> save(User user) {
        return repository.save(mapper.toEntity(user))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<User> update(User user) {
        return repository.update(mapper.toEntity(user))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<User> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndEmail(UUID tenantId, String email) {
        return repository.existsByTenantIdAndEmail(tenantId, email);
    }

    @Override
    public Flux<User> findByTenantId(UUID tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<User> findByTenantIdAndStatus(UUID tenantId, UserStatus status, Pageable pageable) {
        return repository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(mapper::toDomain);
    }
}