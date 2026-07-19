package com.thinklab.application.interactor;

import com.thinklab.application.port.in.GetUserUseCase;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.query.GetUserQuery;
import com.thinklab.domain.exception.BusinessException;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link GetUserUseCase} input port.
 * This service provides secure, read-only access to Enterprise Identities, strictly
 * following the CQRS principle. It ensures that identity retrieval is performed
 * within the boundaries of multi-tenant isolation and signals appropriate
 * domain exceptions for non-existent records.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Data Isolation:</b> Enforces tenant-based scoping to prevent cross-tenant leakage.</li>
 *     <li><b>Reactive Flow:</b> Non-blocking execution utilizing Project Reactor.</li>
 *     <li><b>Zero Side-Effects:</b> Pure retrieval logic with no state mutation or audit writes.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class GetUserInteractor implements GetUserUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * Explicit constructor injection to ensure AOT compliance and proxy resilience.
     */
    @Inject
    public GetUserInteractor(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executes the secure retrieval orchestration.
     *
     * @param query The validated {@link GetUserQuery} containing target ID and Tenant context.
     * @return A {@link Mono} emitting the found {@link User} aggregate.
     * @throws BusinessException if the user does not exist or tenant mismatch occurs.
     */
    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull GetUserQuery query) {
        Objects.requireNonNull(query, "GetUserQuery cannot be null");

        return userRepository.findById(query.userId())
                .filter(user -> user.tenantId().equals(query.tenantId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: GET_USER] [ID: {}] [TENANT: {}] - Retrieval halted: Identity not found in this context.",
                            query.userId(), query.tenantId());
                    return Mono.error(new BusinessException("USER_NOT_FOUND",
                            "The requested identity does not exist within the specified organizational context."));
                }))
                .doOnSubscribe(s -> log.info("[ACTION: GET_USER] [ID: {}] - Initiating secure retrieval pipeline.", query.userId()))
                .doOnSuccess(user -> {
                    if (user != null) {
                        log.info("[ACTION: GET_USER] [ID: {}] - Identity metadata successfully retrieved and projected.", user.id());
                    }
                })
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: GET_USER] [ID: {}] - Critical failure during data retrieval: {}",
                                query.userId(), error.getMessage());
                    }
                });
    }
}