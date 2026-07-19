package com.thinklab.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Value Object: Type-Safe Enumeration representing the lifecycle states of an Enterprise Identity.
 * Implements a Finite State Machine (FSM) to strictly govern account transitions,
 * ensuring forensic compliance and preventing unauthorized operational states.
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>State Integrity:</b> Explicitly defines allowed movement between states.</li>
 *     <li><b>Zero Trust:</b> REVOKED is a terminal state with no possible egress.</li>
 *     <li><b>AOT Optimization:</b> Pre-computed lookups for high-performance reactive pipelines.</li>
 * </ul>
 */
@Serdeable
@Introspected
public enum UserStatus {

    /** Account is fully operational. */
    ACTIVE,

    /** Account is temporarily disabled by administrative action. */
    SUSPENDED,

    /** Account is locked due to security policy violations (e.g., failed attempts). */
    BLOCKED,

    /** Account is in the process of initial provisioning (requires MFA setup). */
    PENDING_ACTIVATION,

    /** Account is permanently and irreversibly decommissioned. */
    REVOKED;

    /**
     * Map defining the strictly allowed state transitions to protect business invariants.
     */
    private static final Map<UserStatus, Set<UserStatus>> VALID_TRANSITIONS = Map.of(
            PENDING_ACTIVATION, Set.of(ACTIVE, REVOKED),
            ACTIVE, Set.of(SUSPENDED, BLOCKED, REVOKED),
            SUSPENDED, Set.of(ACTIVE, REVOKED),
            BLOCKED, Set.of(ACTIVE, REVOKED),
            REVOKED, Collections.emptySet() // Terminal state
    );

    private static final Map<String, UserStatus> LOOKUP_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    /**
     * Validates if a transition from the current state to the target state is legally permitted.
     *
     * @param targetStatus The intended next state for the user.
     * @throws IllegalArgumentException if the targetStatus is null.
     * @throws IllegalStateException if the transition violates the domain's state machine.
     */
    public void validateTransitionTo(@Nonnull UserStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null.");
        }

        if (this == targetStatus) {
            return; // Idempotent operation
        }

        if (!VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(targetStatus)) {
            throw new IllegalStateException(String.format(
                    "Compliance Violation: Illegal identity transition attempt from [%s] to [%s].",
                    this.name(), targetStatus.name()
            ));
        }
    }

    /**
     * Checks if the status allows the user to perform authentication/login.
     *
     * @return true if the identity is in an operational state.
     */
    public boolean isLoginAllowed() {
        return this == ACTIVE || this == PENDING_ACTIVATION;
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static UserStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        UserStatus status = LOOKUP_MAP.get(value.trim().toUpperCase());
        if (status == null) {
            throw new IllegalArgumentException("Unrecognized identity status: " + value);
        }
        return status;
    }
}