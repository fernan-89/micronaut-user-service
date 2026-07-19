package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.UpdateUserStatusCommand;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for governing state transitions within the
 * Enterprise Identity (User) lifecycle.
 * This interface defines the formal contract for executing lifecycle mutations,
 * ensuring that every status change (e.g., ACTIVE to SUSPENDED) is validated
 * by the Domain State Machine and historically documented for compliance.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the
 * Core Application Logic from inbound adapters such as REST Controllers
 * or Message Listeners.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>State Integrity:</b> Enforces strict state machine transitions through
 *         the aggregate root before committing mutations to persistence.</li>
 *     <li><b>Forensic Compliance:</b> Mandates a business justification (reason)
 *         for every lifecycle event to satisfy strict audit requirements.</li>
 *     <li><b>Reactive Sovereignty:</b> Utilizes Project Reactor types to maintain
 *         non-blocking execution across the identity cluster.</li>
 * </ul>
 */
public interface UpdateUserStatusUseCase {

    /**
     * Orchestrates the state transition workflow for a specific user identity.
     * This includes verifying identity existence, validating the legality of the
     * transition via the Domain State Machine, and atomically persisting the
     * synchronized state alongside its mandatory forensic audit counterpart.
     *
     * @param command The {@link UpdateUserStatusCommand} containing the target ID,
     *                the intended new status, and compliance metadata.
     * @return A {@link Mono} emitting the mutated {@link User} aggregate state.
     * @throws NullPointerException if the provided command is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@code UserNotFoundException} signal through the reactive
     *          pipeline if the target identifier is not found in the registry.
     */
    @Nonnull
    Mono<User> execute(@Nonnull UpdateUserStatusCommand command);
}