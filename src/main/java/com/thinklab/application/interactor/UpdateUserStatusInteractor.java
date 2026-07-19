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
 * This service orchestrates the transition of a user's operational status (State Machine).
 * It ensures that lifecycle mutations are strictly validated by domain invariants,
 * synchronized with the persistent store using safe update semantics, and
 * historically documented via a mandatory forensic audit trail.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>State Machine Governance:</b> Transitions are enforced by the Domain Aggregate.</li>
 *     <li><b>Atomic Compliance:</b> Mutations and Audits are logically bound in a reactive chain.</li>
 *     <li><b>Forensic Integrity:</b> Mandatory business justification for every state change.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class UpdateUserStatusInteractor implements UpdateUserStatusUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    /**
     * Explicit constructor injection to satisfy AOT requirements and DI transparency.
     */
    @Inject
    public UpdateUserStatusInteractor(UserRepositoryPort userRepository,
                                      UserAuditRepositoryPort auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    /**
     * Executes the status transition orchestration.
     *
     * @param command The validated intent to transition user state.
     * @return A {@link Mono} emitting the mutated {@link User} aggregate.
     * @throws BusinessException if the user is not found or transition is illegal.
     */
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
                .map(existingUser -> existingUser.transitionTo(command.status(), command.executor(), command.reason()))
                .flatMap(userRepository::update)
                .flatMap(updatedUser -> registerForensicAudit(updatedUser, command.executor(), command.reason())
                        .thenReturn(updatedUser))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_USER_STATUS] [ID: {}] [TARGET: {}] - Initiating lifecycle transition protocol.",
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

    /**
     * Internal logic to persist the immutable forensic audit for the state change.
     */
    private Mono<UserAudit> registerForensicAudit(User user, String executor, String reason) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId().toString(),
                user.id().toString(),
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