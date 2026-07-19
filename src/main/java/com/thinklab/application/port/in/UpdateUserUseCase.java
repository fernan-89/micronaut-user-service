package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.UpdateUserCommand;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for updating the biographical profile of an existing
 * Enterprise Identity (User).
 * This interface defines the contract for orchestrating metadata mutations, ensuring
 * that business invariants are preserved and forensic audit trails are recorded
 * for compliance.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the core
 * application logic from inbound adapters like REST Controllers or Message Listeners.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Domain Integrity:</b> Ensures that updates pass through the aggregate root
 *         to validate business invariants before persistence.</li>
 *     <li><b>Forensic Traceability:</b> Mandates that every successful update results
 *         in an immutable forensic audit entry.</li>
 *     <li><b>Reactive Flow:</b> Strictly utilizes Project Reactor types to maintain
 *         non-blocking execution across the IAM ecosystem.</li>
 * </ul>
 */
public interface UpdateUserUseCase {

    /**
     * Orchestrates the complex workflow of updating a user's profile metadata.
     * This includes locating the existing identity, applying the functional mutation
     * through the aggregate root, and committing the synchronized state alongside
     * a mandatory audit log.
     *
     * @param command The {@link UpdateUserCommand} carrying the target identifier,
     *                new profile data, and authorized executor metadata.
     * @return A {@link Mono} emitting the updated {@link User} aggregate state.
     * @throws NullPointerException if the provided command is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@code UserNotFoundException} signal through the reactive
     *          pipeline if the target identifier does not exist in the registry.
     */
    @Nonnull
    Mono<User> execute(@Nonnull UpdateUserCommand command);
}