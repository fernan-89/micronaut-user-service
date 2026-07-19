package com.thinklab.application.interactor;

import com.thinklab.application.port.in.UpdateUserStatusUseCase;
import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserStatusCommand;
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
 * Application Interactor: Implementation of the {@link UpdateUserStatusUseCase} input port.
 * Orchestrates identity lifecycle transitions with forensic audit requirements.
 */
@Slf4j
@Singleton
public class UpdateUserStatusInteractor implements UpdateUserStatusUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    @Inject
    public UpdateUserStatusInteractor(UserRepositoryPort userRepository,
                                      UserAuditRepositoryPort auditRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull UpdateUserStatusCommand command) {
        Objects.requireNonNull(command, "UpdateUserStatusCommand cannot be null");

        return userRepository.findById(command.userId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: UPDATE_USER_STATUS] [ID: {}] - Orchestration halted: Entity not found.",
                            command.userId());
                    return Mono.error(new BusinessException("USER_NOT_FOUND",
                            "The target identity does not exist in the organizational registry."));
                }))
                // Transição de estado encapsulada no Agregado de Domínio
                .map(user -> user.transitionTo(command.status(), command.executor(), command.reason()))
                // Repositório abstrai o mapeamento para persistência
                .flatMap(userRepository::update)
                .flatMap(updatedUser -> registerForensicAudit(updatedUser, command.executor(), command.reason())
                        .thenReturn(updatedUser))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_USER_STATUS] [ID: {}] [TARGET: {}] - Initiating lifecycle transition.",
                        command.userId(), command.status()))
                .doOnSuccess(user -> log.info("[ACTION: UPDATE_USER_STATUS] [ID: {}] - Transition successfully committed and audited.",
                        user.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: UPDATE_USER_STATUS] [ID: {}] - Critical failure during state transition: {}",
                                command.userId(), error.getMessage());
                    }
                });
    }

    private Mono<UserAudit> registerForensicAudit(User user, String executor, String reason) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId(),
                user.id(),
                "USER_STATUS_TRANSITION",
                "SUCCESS",
                executor,
                Map.of(
                        "newStatus", user.status().name(),
                        "reason", reason,
                        "version", String.valueOf(user.version()),
                        "traceabilityTier", "3"
                )
        );
        return auditRepository.save(auditEntry);
    }
}