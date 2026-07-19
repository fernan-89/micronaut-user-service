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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private final UpdateUserCommand command = new UpdateUserCommand(userId, newProfile, "admin-agent");

    @BeforeEach
    void setUp() {
        // Agora alinhado com o construtor da classe principal
        interactor = new UpdateUserInteractor(userRepository, auditRepository);
    }

    @Test
    @DisplayName("Should successfully update user profile and register forensic audit")
    void shouldUpdateSuccessfully() {
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR,
                new UserProfile("John Doe", "john@t.com", null, "pt-br", "UTC"), "admin");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any(User.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(auditRepository.save(any())).thenReturn(Mono.empty());

        var result = interactor.execute(command);

        StepVerifier.create(result)
                .assertNext(updatedUser -> {
                    assertEquals("John Updated", updatedUser.profile().fullName());
                    assertEquals("admin-agent", updatedUser.updatedBy());
                })
                .verifyComplete();

        verify(userRepository, times(1)).update(any(User.class));
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should emit UserNotFoundException when target identity is missing")
    void shouldFailWhenUserNotFound() {
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.empty());

        var result = interactor.execute(command);

        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should propagate infrastructure errors and skip audit sequence")
    void shouldPropagatePersistenceError() {
        var existingUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR,
                new UserProfile("John Doe", "john@t.com", null, "pt-br", "UTC"), "admin");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(existingUser));
        when(userRepository.update(any())).thenReturn(Mono.error(new RuntimeException("MongoDB Write Timeout")));

        var result = interactor.execute(command);

        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Write Timeout")
                .verify();

        verify(auditRepository, never()).save(any());
    }
}