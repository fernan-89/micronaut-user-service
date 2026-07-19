package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserStatusCommand;
import com.thinklab.domain.exception.InvalidUserStateTransitionException;
import com.thinklab.domain.exception.UserNotFoundException;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Application Unit Test: Validates the lifecycle orchestration logic of the {@link UpdateUserStatusInteractor}.
 * This suite ensures that state transitions are strictly governed by domain FSM rules,
 * committed to persistence, and transactionally linked to the mandatory forensic audit trail.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: UpdateUserStatus Interactor")
class UpdateUserStatusInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserAuditRepositoryPort auditRepository;

    private UpdateUserStatusInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String executor = "admin-agent-01";
    private final String reason = "Administrative suspension for compliance review.";

    @BeforeEach
    void setUp() {
        interactor = new UpdateUserStatusInteractor(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should successfully transition user status and register forensic audit")
    void shouldUpdateStatusSuccessfully() {
        // Given: An active user identity
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var activeUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "system")
                .activate("system");

        var command = new UpdateUserStatusCommand(tenantId, userId, UserStatus.SUSPENDED, executor, reason);

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(activeUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(auditRepository.save(any())).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .assertNext(updatedUser -> {
                    assert updatedUser.status() == UserStatus.SUSPENDED;
                    assert updatedUser.updatedBy().equals(executor);
                })
                .verifyComplete();

        verify(userRepository, times(1)).update(any(User.class));
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail when identity is missing in organizational context")
    void shouldFailWhenUserNotFound() {
        // Given
        var command = new UpdateUserStatusCommand(tenantId, userId, UserStatus.ACTIVE, executor, reason);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, never()).update(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when domain machine prohibits the transition (e.g., ARCHIVED -> ACTIVE)")
    void shouldFailOnIllegalTransition() {
        // Given: An archived user
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var archivedUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "system")
                .activate("system")
                .archive("system");

        var command = new UpdateUserStatusCommand(tenantId, userId, UserStatus.ACTIVE, executor, "Illegal attempt");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(archivedUser));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectError(InvalidUserStateTransitionException.class)
                .verify();

        verify(userRepository, never()).update(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail fast with NullPointerException if command is null")
    void shouldFailFastOnNullCommand() {
        assertThrows(NullPointerException.class, () -> interactor.execute(null));
        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should propagate infrastructure errors and abort audit trail")
    void shouldPropagatePersistenceError() {
        // Given
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var activeUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "system");
        var command = new UpdateUserStatusCommand(tenantId, userId, UserStatus.ACTIVE, executor, reason);

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(activeUser));
        when(userRepository.update(any())).thenReturn(Mono.error(new RuntimeException("MongoDB Read-Only Mode")));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Read-Only Mode")
                .verify();

        verify(auditRepository, never()).save(any());
    }
}