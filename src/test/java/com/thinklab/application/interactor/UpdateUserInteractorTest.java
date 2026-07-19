package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserCommand;
import com.thinklab.domain.exception.UserNotFoundException;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
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
 * Application Unit Test: Validates the orchestration logic of the {@link UpdateUserInteractor}.
 * This suite ensures that biographical mutations are only committed if the identity
 * exists, and that every successful state change is transactionally linked to
 * a mandatory forensic audit trail for security compliance.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: UpdateUser Interactor")
class UpdateUserInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserAuditRepositoryPort auditRepository;

    private UpdateUserInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UserProfile newProfile = new UserProfile(
            "John Updated", "john.upd@thinklab.com", "+5511988887777", "en", "UTC"
    );
    private final UpdateUserCommand command = new UpdateUserCommand(tenantId, userId, newProfile, "admin-agent");

    @BeforeEach
    void setUp() {
        interactor = new UpdateUserInteractor(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should successfully update user profile and register forensic audit")
    void shouldUpdateSuccessfully() {
        // Given: An existing user identity
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR,
                new UserProfile("John Doe", "john@t.com", null, "pt-br", "UTC"), "admin");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(auditRepository.save(any())).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .assertNext(updatedUser -> {
                    assert updatedUser.profile().fullName().equals("John Updated");
                    assert updatedUser.updatedBy().equals("admin-agent");
                })
                .verifyComplete();

        verify(userRepository, times(1)).update(any(User.class));
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should emit UserNotFoundException when target identity is missing")
    void shouldFailWhenUserNotFound() {
        // Given: Repository signal for non-existent identity
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
    @DisplayName("Should fail fast with NullPointerException if command is null")
    void shouldFailFastOnNullCommand() {
        assertThrows(NullPointerException.class, () -> interactor.execute(null));
        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should propagate infrastructure errors and skip audit sequence")
    void shouldPropagatePersistenceError() {
        // Given: A critical database failure
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR,
                new UserProfile("John Doe", "john@t.com", null, "pt-br", "UTC"), "admin");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any())).thenReturn(Mono.error(new RuntimeException("MongoDB Write Timeout")));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Write Timeout")
                .verify();

        verify(auditRepository, never()).save(any());
    }
}