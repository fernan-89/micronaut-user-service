package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ProvisionUserCommand;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the provisioning of new Enterprise Identities (Users).
 * This interface defines the contract for establishing a user's presence within a
 * hierarchical tenant structure, ensuring that business rules, algorithmic
 * assignments, and forensic audit requirements are satisfied.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the Core
 * Application Logic from inbound adapters (e.g., REST Controllers).</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Domain Integrity:</b> Enforces hierarchical tenant boundaries during
 *         provisioning.</li>
 *     <li><b>Forensic Compliance:</b> Mandates that all successful creations result
 *         in an immutable audit entry.</li>
 *     <li><b>Reactive Flow:</b> Strictly utilizes Project Reactor types to maintain
 *         non-blocking execution across the IAM ecosystem.</li>
 * </ul>
 */
public interface ProvisionUserUseCase {

    /**
     * Orchestrates the complex workflow of provisioning a new user identity.
     * This includes uniqueness validation within the tenant context, initial state
     * assignment, and the atomic persistence of both the identity and its
     * corresponding audit trail.
     *
     * @param command The {@link ProvisionUserCommand} encapsulating validated
     *                identity metadata and organizational context.
     * @return A {@link Mono} emitting the successfully provisioned {@link User} aggregate.
     * @throws NullPointerException if the provided command is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@code BusinessException} signal (e.g., "USER_ALREADY_EXISTS")
     *          if a conflict is detected at the domain level.
     */
    @Nonnull
    Mono<User> execute(@Nonnull ProvisionUserCommand command);
}

