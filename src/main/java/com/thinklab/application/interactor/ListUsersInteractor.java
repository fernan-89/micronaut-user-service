package com.thinklab.application.interactor;

import com.thinklab.application.port.in.ListUsersUseCase;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.query.ListUsersQuery;
import com.thinklab.domain.model.User;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link ListUsersUseCase} input port.
 * This service orchestrates the high-performance retrieval of user identities,
 * strictly adhering to the CQRS principle for read-only operations. It enforces
 * hierarchical tenant isolation and utilizes reactive pagination to ensure
 * system stability under high-concurrency discovery requests.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Data Isolation:</b> Mandatory tenant-based scoping for every query.</li>
 *     <li><b>Reactive Streaming:</b> Zero-blocking execution with Flux backpressure.</li>
 *     <li><b>Selective Retrieval:</b> Dynamic routing between status-filtered and
 *         unfiltered tenant streams.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class ListUsersInteractor implements ListUsersUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * Explicit constructor injection to prevent proxy-related instantiation
     * failures (AOT Ready).
     */
    @Inject
    public ListUsersInteractor(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executes the paginated discovery orchestration.
     *
     * @param query The validated {@link ListUsersQuery} containing tenant context
     *              and pagination metadata.
     * @return A {@link Flux} streaming the found {@link User} aggregates.
     * @throws NullPointerException if the provided query is null (Fail-Fast).
     */
    @Override
    @Nonnull
    public Flux<User> execute(@Nonnull ListUsersQuery query) {
        Objects.requireNonNull(query, "ListUsersQuery cannot be null");

        // Micronaut Data Pageable instantiation
        Pageable pageable = Pageable.from(query.page(), query.size());

        return Flux.defer(() -> {
                    log.info("[ACTION: LIST_USERS] [TENANT: {}] [STATUS: {}] [PAGE: {}] - " +
                                    "Initiating secure discovery stream.",
                            query.tenantId(),
                            (query.status() != null ? query.status() : "ALL"),
                            query.page());

                    if (query.status() != null) {
                        return userRepository.findByTenantIdAndStatus(
                                query.tenantId(),
                                query.status(),
                                pageable
                        );
                    }

                    return userRepository.findByTenantId(query.tenantId(), pageable);
                })
                .doOnComplete(() -> log.debug("[ACTION: LIST_USERS] [TENANT: {}] - Discovery pipeline completed.",
                        query.tenantId()))
                .doOnError(error -> log.error("[ACTION: LIST_USERS] [TENANT: {}] - Discovery failure: {}",
                        query.tenantId(), error.getMessage()));
    }
}

