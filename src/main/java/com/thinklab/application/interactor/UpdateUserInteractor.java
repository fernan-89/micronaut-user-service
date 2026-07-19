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
 * This service orchestrates the modification of a user's biographical profile metadata.
 * It strictly separates profile updates from security-critical lifecycle transitions
 * and ensures that every mutation is historically documented for compliance.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Immutable State Management:</b> Functional mutation through the domain aggregate.</li>
 *     <li><b>Forensic Integrity:</b> Mandatory append-only audit trail for every mutation.</li>
 *     <li><b>Reactive Flow:</b> Fully non-blocking execution utilizing Project Reactor.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class UpdateUserInteractor implements UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;

    /**
     * Explicit constructor injection for AOT compliance and dependency clarity.
     */
    @Inject
    public UpdateUserInteractor(UserRepositoryPort userRepository,
                                UserAuditRepositoryPort auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    /**
     * Executes the profile update orchestration pipeline.
     *
     * @param command The validated intent to modify biographical metadata.
     * @return A {@link Mono} emitting the updated {@link User} aggregate state.
     * @throws BusinessException if the target identity is not found.
     */
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
                .map(existingUser -> existingUser.updateProfile(command.profile(), command.executor()))
                .flatMap(userRepository::update)
                .flatMap(updatedUser -> registerForensicAudit(updatedUser, command.executor())
                        .thenReturn(updatedUser))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Initiating metadata mutation protocol.",
                        command.userId()))
                .doOnSuccess(user -> log.info("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Mutation successfully synchronized and audited.",
                        user.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: UPDATE_USER_PROFILE] [ID: {}] - Critical failure during synchronization: {}",
                                command.userId(), error.getMessage());
                    }
                });
    }

    /**
     * Creates and persists the immutable forensic record for the profile update.
     */
    private Mono<UserAudit> registerForensicAudit(User user, String executor) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId().toString(),
                user.id().toString(),
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