package com.thinklab.domain.model;

import com.thinklab.domain.exception.InvalidUserStateTransitionException;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain Unit Test: Validates the core business invariants and state machine
 * of the {@link User} aggregate root.
 */
@DisplayName("Domain: User Aggregate Root")
class UserTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile(
            "John Doe",
            "john.doe@thinklab.com",
            "5511999999999",
            "America/Sao_Paulo",
            "pt-BR"
    );

    @Test
    @DisplayName("Should initialize user in PENDING_ACTIVATION status via factory")
    void shouldInitializeUserInPendingStatus() {
        // Given & When
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");

        // Then
        assertNotNull(user.id());
        assertEquals(UserStatus.PENDING_ACTIVATION, user.status());
        assertEquals(0, user.failedAttempts());
        assertFalse(user.mfaEnabled());
        assertEquals(1, user.roles().size());
        assertTrue(user.roles().contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should return new instance on state transition (Immutability)")
    void shouldReturnNewInstanceOnStateTransition() {
        // Given
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");

        // When
        var activatedUser = user.activate("admin-01");

        // Then
        assertNotSame(user, activatedUser);
        assertEquals(UserStatus.PENDING_ACTIVATION, user.status());
        assertEquals(UserStatus.ACTIVE, activatedUser.status());
    }

    @Test
    @DisplayName("Should throw exception on illegal state transition (REVOKED -> ACTIVE)")
    void shouldThrowExceptionOnIllegalTransition() {
        // Given
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01")
                .activate("admin-01")
                .revoke("admin-01");

        // When & Then
        assertThrows(InvalidUserStateTransitionException.class, () -> user.activate("admin-01"),
                "Should not allow reactivation of a revoked identity");
    }

    @Test
    @DisplayName("Should increment version on mutation")
    void shouldIncrementVersionOnMutation() {
        // Given
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");
        long initialVersion = user.version();

        // When
        var mutatedUser = user.activate("admin-01");

        // Then
        assertEquals(initialVersion + 1, mutatedUser.version());
    }

    @Test
    @DisplayName("Should lock user after 5 failed login attempts")
    void shouldLockUserAfterFiveFailedAttempts() {
        // Given
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01")
                .activate("admin-01");

        // When: Passando 1 como argumento para o método
        var state1 = user.recordFailedLogin(1);
        var state2 = state1.recordFailedLogin(1);
        var state3 = state2.recordFailedLogin(1);
        var state4 = state3.recordFailedLogin(1);
        var finalState = state4.recordFailedLogin(1);

        // Then
        assertEquals(5, finalState.failedAttempts());
        assertEquals(UserStatus.BLOCKED, finalState.status());
    }

    @Test
    @DisplayName("Should update profile and return new instance")
    void shouldUpdateProfile() {
        // Given
        var user = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");
        var newProfile = new UserProfile("John Updated", "new@thinklab.com", "123", "UTC", "en");

        // When
        var updatedUser = user.updateProfile(newProfile, "admin-02");

        // Then
        assertEquals("John Updated", updatedUser.profile().fullName());
        assertEquals("admin-02", updatedUser.updatedBy());
        assertNotNull(updatedUser.updatedAt());
    }
}