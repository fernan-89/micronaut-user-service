package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.UpdateUserRolesCommand;
import com.thinklab.domain.model.User;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for modifying the functional roles and permissions
 * of an existing Enterprise Identity (User).
 * This interface defines the formal contract for orchestrating privilege mutations,
 * ensuring that any change to a user's access matrix is authorized, justified,
 * and historically documented for security compliance.
 *
 * <p>Following the Hexagonal Architecture pattern, this port decouples the core
 * application logic from inbound adapters such as REST Controllers or Identity Brokers.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Least Privilege Integrity:</b> Enforces that role modifications pass
 *         through the aggregate root to maintain domain consistency.</li>
 *     <li><b>Forensic Compliance:</b> Mandates a business justification (reason)
 *         for every authorization change to satisfy strict audit requirements.</li>
 *     <li><b>Reactive Sovereignty:</b> Strictly utilizes Project Reactor types to
 *         maintain non-blocking execution across the identity cluster.</li>
 * </ul>
 */
public interface UpdateUserRolesUseCase {

    /**
     * Orchestrates the complex workflow of updating a user's functional permissions.
     * This includes verifying identity existence, applying the role mutation
     * through the aggregate root, and atomically persisting the synchronized
     * state alongside its mandatory forensic audit counterpart.
     *
     * @param command The {@link UpdateUserRolesCommand} encapsulating the target ID,
     *                the new roles collection, and compliance metadata.
     * @return A {@link Mono} emitting the updated {@link User} aggregate state.
     * @throws NullPointerException if the provided command is null, preserving
     *                              pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@code UserNotFoundException} signal through the reactive
     *          pipeline if the target identifier is not found in the registry.
     */
    @Nonnull
    Mono<User> execute(@Nonnull UpdateUserRolesCommand command);
}