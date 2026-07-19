package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserAuditRepositoryPort;
import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.command.UpdateUserRolesCommand;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Application Unit Test: Validates the security-critical orchestration logic of the
 * {@link UpdateUserRolesInteractor}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: UpdateUserRoles Interactor")
class UpdateUserRolesInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserAuditRepositoryPort auditRepository;

    private UpdateUserRolesInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final List<String> newRoles = List.of("ROLE_ADMIN", "REPORTS_MANAGER");

    // Instanciação correta seguindo a assinatura: (userId, roles, executor, reason)
    private final UpdateUserRolesCommand command = new UpdateUserRolesCommand(
            userId,
            newRoles,
            "security-admin",
            "Forensic upgrade due to department change"
    );

    @BeforeEach
    void setUp() {
        interactor = new UpdateUserRolesInteractor(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should successfully update user roles and register forensic audit trail")
    void shouldUpdateRolesSuccessfully() {
        // Given: An established identity in the tenant context
        var profile = new UserProfile("John Doe", "john@t.com", null, "en", "UTC");
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "system");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(auditRepository.save(any())).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assert user.roles().containsAll(newRoles);
                    assert user.updatedBy().equals("security-admin");
                })
                .verifyComplete();

        verify(userRepository, times(1)).update(any(User.class));
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should emit UserNotFoundException when identity is missing in context")
    void shouldFailWhenUserNotFound() {
        // Given: Repository signal for non-existent resource
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
        assertThrows(NullPointerException.class, () -> interactor.execute(null),
                "Interactor must prevent execution of null security intent objects.");

        verifyNoInteractions(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should propagate persistence error and skip forensic audit sequence")
    void shouldPropagatePersistenceError() {
        // Given: A critical database write failure
        var profile = new UserProfile("John Doe", "john@t.com", null, "en", "UTC");
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "system");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any())).thenReturn(Mono.error(new RuntimeException("MongoDB Access Denied")));

        // When
        var result = interactor.execute(command);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Access Denied")
                .verify();

        verify(auditRepository, never()).save(any());
    }
}