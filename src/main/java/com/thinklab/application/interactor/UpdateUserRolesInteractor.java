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
 * This service orchestrates the modification of functional roles and permissions
 * for an Enterprise Identity. It enforces a strict security boundary, ensuring
 * that any change to a user's access matrix is validated by domain invariants,
 * committed using optimistic locking, and documented via a mandatory forensic
 * audit trail for Tier 3 compliance.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Least Privilege Governance:</b> Mutations are enforced by the Aggregate Root.</li>
 *     <li><b>Forensic Integrity:</b> Mandatory business justification for every role change.</li>
 *     <li><b>Reactive Sovereignty:</b> Zero-blocking execution utilizing Project Reactor.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class UpdateUserRolesInteractor implements UpdateUserRolesUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    /**
     * Explicit constructor injection to satisfy NASA-level DI requirements
     * and ensure Ahead-of-Time (AOT) compatibility.
     */
    @Inject
    public UpdateUserRolesInteractor(UserRepositoryPort userRepository,
                                     UserAuditRepositoryPort auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    /**
     * Executes the privilege mutation orchestration.
     *
     * @param command The validated intent to update user roles.
     * @return A {@link Mono} emitting the mutated {@link User} aggregate state.
     * @throws BusinessException if the user is not found or domain invariants are violated.
     */
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
                .map(existingUser -> existingUser.updateRoles(command.roles(), command.executor(), command.reason()))
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

    /**
     * Internal logic to persist the immutable forensic audit for the authorization change.
     */
    private Mono<UserAudit> registerForensicAudit(User user, String executor, String reason) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId().toString(),
                user.id().toString(),
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