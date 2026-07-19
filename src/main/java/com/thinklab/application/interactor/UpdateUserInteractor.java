package com.thinklab.application.interactor;

import com.thinklab.application.port.in.UpdateUserUseCase;
import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserCommand;
import com.thinklab.domain.exception.BusinessException;
import com.thinklab.domain.model.User;
import com.thinklab.domain.model.UserAudit;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link UpdateUserUseCase} input port.
 * Orchestrates the mutation of identity metadata with strict auditability.
 */
@Slf4j
@Singleton
public class UpdateUserInteractor implements UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    @Inject
    public UpdateUserInteractor(UserRepositoryPort userRepository,
                                UserAuditRepositoryPort auditRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull UpdateUserCommand command) {
        Objects.requireNonNull(command, "UpdateUserCommand cannot be null");

        return userRepository.findById(command.userId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Orchestration halted: Entity not found.",
                            command.userId());
                    return Mono.error(new BusinessException("USER_NOT_FOUND",
                            "The specified identity does not exist in the organizational registry."));
                }))
                // A lógica de negócio reside no objeto de domínio
                .map(user -> user.updateProfile(command.profile(), command.executor()))
                // Persistência delegada: o adaptador cuida do mapeamento para Entidade
                .flatMap(userRepository::update)
                .flatMap(updatedUser -> registerForensicAudit(updatedUser, command.executor())
                        .thenReturn(updatedUser))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Initiating metadata mutation.",
                        command.userId()))
                .doOnSuccess(user -> log.info("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Mutation synchronized and audited.",
                        user.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Critical synchronization failure: {}",
                                command.userId(), error.getMessage());
                    }
                });
    }

    private Mono<UserAudit> registerForensicAudit(User user, String executor) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId(),
                user.id(),
                "USER_PROFILE_UPDATE",
                "SUCCESS",
                executor,
                Map.of(
                        "email", user.profile().corporateEmail(),
                        "language", user.profile().preferredLanguage(),
                        "timezone", user.profile().timezone(),
                        "traceabilityTier", "3"
                )
        );
        return auditRepository.save(auditEntry);
    }
}