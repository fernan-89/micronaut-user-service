package com.thinklab.application.interactor;

import com.thinklab.application.port.in.UpdateUserRolesUseCase;
import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserRolesCommand;
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
 * Application Interactor: Implementation of the {@link UpdateUserRolesUseCase} input port.
 * Orchestrates security-sensitive privilege mutations with strict forensic integrity.
 */
@Slf4j
@Singleton
public class UpdateUserRolesInteractor implements UpdateUserRolesUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    @Inject
    public UpdateUserRolesInteractor(UserRepositoryPort userRepository,
                                     UserAuditRepositoryPort auditRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull UpdateUserRolesCommand command) {
        Objects.requireNonNull(command, "UpdateUserRolesCommand cannot be null");

        return userRepository.findById(command.userId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: UPDATE_USER_ROLES] [ID: {}] - Orchestration halted: Entity not found.",
                            command.userId());
                    return Mono.error(new BusinessException("USER_NOT_FOUND",
                            "The target identity does not exist in the organizational registry."));
                }))
                // A lógica de autorização é aplicada no agregado de domínio
                .map(user -> user.updateRoles(command.roles(), command.executor(), command.reason()))
                // Persistência delegada: o repositório lida com o mapeamento para Entidade
                .flatMap(userRepository::update)
                .flatMap(updatedUser -> registerForensicAudit(updatedUser, command.executor(), command.reason())
                        .thenReturn(updatedUser))
                .doOnSubscribe(s -> log.warn("[ACTION: UPDATE_USER_ROLES] [ID: {}] - Initiating security-sensitive privilege mutation.",
                        command.userId()))
                .doOnSuccess(user -> log.info("[ACTION: UPDATE_USER_ROLES] [ID: {}] - Roles successfully synchronized and audited.",
                        user.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: UPDATE_USER_ROLES] [ID: {}] - Critical failure during authorization update: {}",
                                command.userId(), error.getMessage());
                    }
                });
    }

    private Mono<UserAudit> registerForensicAudit(User user, String executor, String reason) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId(),
                user.id(),
                "USER_ROLES_UPDATE",
                "SUCCESS",
                executor,
                Map.of(
                        "newRoles", String.join(",", user.roles()),
                        "reason", reason,
                        "version", String.valueOf(user.version()),
                        "traceabilityTier", "3"
                )
        );
        return auditRepository.save(auditEntry);
    }
}