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

@Slf4j
@Singleton
public class GetUserInteractor implements GetUserUseCase {

    private final UserRepositoryPort userRepository;

    @Inject
    public GetUserInteractor(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull GetUserQuery query) {
        Objects.requireNonNull(query, "GetUserQuery cannot be null");

        // O Repositório agora retorna Mono<User> (Domínio puro)
        return userRepository.findById(query.userId())
                .filter(user -> user.tenantId().equals(query.tenantId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: GET_USER] [ID: {}] [TENANT: {}] - Retrieval halted: Identity not found.",
                            query.userId(), query.tenantId());
                    return Mono.error(new BusinessException("USER_NOT_FOUND",
                            "The requested identity does not exist within the specified organizational context."));
                }))
                .doOnSubscribe(s -> log.info("[ACTION: GET_USER] [ID: {}] - Initiating secure retrieval pipeline.", query.userId()))
                .doOnSuccess(user -> log.info("[ACTION: GET_USER] [ID: {}] - Identity successfully retrieved.", user.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: GET_USER] [ID: {}] - Critical failure: {}", query.userId(), error.getMessage());
                    }
                });
    }
}