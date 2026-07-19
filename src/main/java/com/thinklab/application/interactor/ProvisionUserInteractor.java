package com.thinklab.application.interactor;

import com.thinklab.application.port.in.ProvisionUserUseCase;
import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.ProvisionUserCommand;
import com.thinklab.domain.exception.BusinessException;
import com.thinklab.domain.model.User;
import com.thinklab.domain.model.UserAudit;
import com.thinklab.infrastructure.adapter.out.mongo.mapper.UserMapper;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link ProvisionUserUseCase} input port.
 * This service orchestrates the complex workflow of establishing a new Enterprise Identity.
 * It enforces business uniqueness within tenant boundaries, manages the initial
 * state assignment via the aggregate root, and guarantees an immutable
 * forensic audit trail for every successful provisioning.
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Reactive Sovereignty:</b> Zero-blocking execution using Project Reactor.</li>
 *     <li><b>Atomic Integrity:</b> Encapsulates persistence and auditing in a single chain.</li>
 *     <li><b>Defensive Boundary:</b> Validates uniqueness before CPU-intensive instantiation.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class ProvisionUserInteractor implements ProvisionUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAuditRepositoryPort auditRepository;
    private final UserMapper userMapper;

    /**
     * Explicit constructor injection to ensure AOT compatibility and DI resilience.
     */
    @Inject
    public ProvisionUserInteractor(UserRepositoryPort userRepository,
                                   UserAuditRepositoryPort auditRepository,
                                   UserMapper userMapper) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.userMapper = userMapper;
    }

    /**
     * Executes the provisioning orchestration pipeline.
     *
     * @param command The validated intent to create a new user.
     * @return A {@link Mono} emitting the successfully provisioned {@link User}.
     * @throws BusinessException if an identity conflict is detected.
     */
    @Override
    @Nonnull
    public Mono<User> execute(@Nonnull ProvisionUserCommand command) {
        Objects.requireNonNull(command, "ProvisionUserCommand cannot be null");

        return userRepository.existsByTenantIdAndEmail(command.tenantId(), command.email())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("[ACTION: PROVISION_USER] [TENANT: {}] - Orchestration halted: Email conflict detected.",
                                command.tenantId());
                        return Mono.error(new BusinessException("USER_ALREADY_EXISTS",
                                "An identity with this corporate email is already registered in this organization."));
                    }
                    return performProvisioning(command);
                })
                .doOnSubscribe(s -> log.info("[ACTION: PROVISION_USER] [TENANT: {}] - Initiating identity creation protocol.",
                        command.tenantId()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: PROVISION_USER] [TENANT: {}] - Critical failure during provisioning: {}",
                                command.tenantId(), error.getMessage());
                    }
                });
    }

    /**
     * Internal logic for aggregate instantiation and dual-port persistence.
     */
    private Mono<User> performProvisioning(ProvisionUserCommand command) {
        User newUser = User.provision(
                command.tenantId(),
                command.parentId(),
                command.username(),
                command.level(),
                command.profile(),
                command.executor()
        );

        // O repository já recebe o domínio (User) e retorna o domínio (User)
        // O Adapter dentro da infraestrutura cuida da conversão toEntity/toDomain
        return userRepository.save(newUser)
                .flatMap(savedUser -> registerForensicAudit(savedUser, command.executor())
                        .thenReturn(savedUser))
                .doOnSuccess(user -> log.info("[ACTION: PROVISION_USER] [ID: {}] - Provisioning successfully completed.",
                        user.id()));
    }

    /**
     * Creates and persists the immutable forensic record for the creation event.
     */
    private Mono<UserAudit> registerForensicAudit(User user, String executor) {
        UserAudit auditEntry = UserAudit.create(
                user.tenantId(),
                user.id(), // UUID passado diretamente
                "USER_PROVISIONING",
                "SUCCESS",
                executor,
                Map.of(
                        "level", user.level().name(),
                        "username", user.username(),
                        "initialStatus", user.status().name(),
                        "traceabilityTier", "3"
                )
        );
        return auditRepository.save(auditEntry);
    }
}