package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.ProvisionUserCommand;
import com.thinklab.domain.exception.UserAlreadyExistsException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Application Unit Test: Validates the orchestration logic of the {@link ProvisionUserInteractor}.
 * This suite ensures that the provisioning pipeline correctly enforces uniqueness,
 * persists the identity aggregate, and generates the mandatory forensic audit trail
 * required for Tier 3 compliance.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: ProvisionUser Interactor")
class ProvisionUserInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserAuditRepositoryPort auditRepository;

    private ProvisionUserInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile("John Doe", "john.doe@thinklab.com", null, "en", "UTC");
    private final ProvisionUserCommand command = new ProvisionUserCommand(
            tenantId, null, "john.doe", "john.doe@thinklab.com", UserLevel.OPERATOR, profile, "admin-01"
    );

    @BeforeEach
    void setUp() {
        interactor = new ProvisionUserInteractor(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should successfully provision user and register audit trail")
    void shouldProvisionSuccessfully() {
        // Given
        when(userRepository.existsByUsername(command.username())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(auditRepository.save(any())).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assert user.username().equals(command.username());
                    assert user.status() == UserStatus.PENDING;
                    assert user.version() == 0L;
                })
                .verifyComplete();

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail when username already exists in tenant context")
    void shouldFailWhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername(command.username())).thenReturn(Mono.just(true));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectError(UserAlreadyExistsException.class)
                .verify();

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when email already exists in organizational context")
    void shouldFailWhenEmailExists() {
        // Given
        when(userRepository.existsByUsername(command.username())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(true));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectError(UserAlreadyExistsException.class)
                .verify();

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate error when persistence layer fails")
    void shouldPropagatePersistenceError() {
        // Given
        when(userRepository.existsByUsername(command.username())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB Connection Lost")));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("DB Connection Lost")
                .verify();

        verify(auditRepository, never()).save(any());
    }
}