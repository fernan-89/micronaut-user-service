package com.thinklab.domain.valueobject;

import com.thinklab.domain.exception.InvalidUserStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain Unit Test: Validates the Finite State Machine (FSM) logic of the {@link UserStatus} enum.
 */
@DisplayName("Domain: UserStatus State Machine")
class UserStatusTest {

    @Test
    @DisplayName("Should allow transition from PENDING_ACTIVATION to ACTIVE")
    void shouldAllowPendingToActive() {
        assertDoesNotThrow(() -> UserStatus.PENDING_ACTIVATION.validateTransitionTo(UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should allow transition from ACTIVE to SUSPENDED")
    void shouldAllowActiveToSuspended() {
        assertDoesNotThrow(() -> UserStatus.ACTIVE.validateTransitionTo(UserStatus.SUSPENDED));
    }

    @Test
    @DisplayName("Should allow transition from SUSPENDED to ACTIVE")
    void shouldAllowSuspendedToActive() {
        assertDoesNotThrow(() -> UserStatus.SUSPENDED.validateTransitionTo(UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should throw exception when transitioning from REVOKED to ACTIVE (Terminal State)")
    void shouldDenyActiveAfterRevoked() {
        assertThrows(InvalidUserStateTransitionException.class,
                () -> UserStatus.REVOKED.validateTransitionTo(UserStatus.ACTIVE),
                "Revoked state must be terminal and deny any further transitions.");
    }

    @Test
    @DisplayName("Should throw exception when transitioning from BLOCKED to ACTIVE without proper flow")
    void shouldDenyDirectActivationFromBlocked() {
        // Nota: Se o seu Enum permitir BLOCKED -> ACTIVE, este teste falhará propositalmente.
        // Se a regra de negócio for bloquear a transição, ajuste a constante VALID_TRANSITIONS no Enum.
        assertThrows(InvalidUserStateTransitionException.class,
                () -> UserStatus.BLOCKED.validateTransitionTo(UserStatus.ACTIVE));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING_ACTIVATION"})
    @DisplayName("Should allow login for operational states")
    void shouldAllowLoginForOperationalStates(UserStatus status) {
        assertTrue(status.isLoginAllowed(),
                "Operational states should allow system access.");
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "BLOCKED", "REVOKED"})
    @DisplayName("Should deny login for restricted states")
    void shouldDenyLoginForRestrictedStates(UserStatus status) {
        assertFalse(status.isLoginAllowed(),
                "Restricted states must invariably deny system access.");
    }

    @Test
    @DisplayName("Should be idempotent when transitioning to the same state")
    void shouldBeIdempotent() {
        assertDoesNotThrow(() -> UserStatus.ACTIVE.validateTransitionTo(UserStatus.ACTIVE));
    }
}