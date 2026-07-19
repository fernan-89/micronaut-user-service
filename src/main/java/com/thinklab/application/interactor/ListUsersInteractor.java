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
 *
 * Orchestrates the retrieval of user entities within a specific tenant boundary.
 * Enforces clean separation between application query models and domain aggregate roots.
 */
@Slf4j
@Singleton
public class ListUsersInteractor implements ListUsersUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * Explicit constructor injection to ensure DI resilience.
     */
    @Inject
    public ListUsersInteractor(UserRepositoryPort userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepositoryPort cannot be null");
    }

    /**
     * Executes the user discovery pipeline.
     *
     * @param query The query parameters for pagination and filtering.
     * @return A {@link Flux} emitting the matching {@link User} domain objects.
     */
    @Override
    @Nonnull
    public Flux<User> execute(@Nonnull ListUsersQuery query) {
        Objects.requireNonNull(query, "ListUsersQuery cannot be null");

        Pageable pageable = Pageable.from(query.page(), query.size());

        return Flux.defer(() -> {
                    log.info("[ACTION: LIST_USERS] [TENANT: {}] [FILTER: {}] [PAGE: {}] - " +
                                    "Initiating secure discovery stream.",
                            query.tenantId(),
                            (query.status() != null ? query.status() : "ALL"),
                            query.page());

                    return fetchUsers(query, pageable);
                })
                .doOnComplete(() -> log.debug("[ACTION: LIST_USERS] [TENANT: {}] - Discovery pipeline drained successfully.",
                        query.tenantId()))
                .doOnError(error -> log.error("[ACTION: LIST_USERS] [TENANT: {}] - Discovery pipeline failure: {}",
                        query.tenantId(), error.getMessage()));
    }

    /**
     * Helper method to encapsulate conditional repository delegation.
     */
    private Flux<User> fetchUsers(ListUsersQuery query, Pageable pageable) {
        if (query.status() != null) {
            return userRepository.findByTenantIdAndStatus(
                    query.tenantId(),
                    query.status(),
                    pageable
            );
        }
        return userRepository.findByTenantId(query.tenantId(), pageable);
    }
}