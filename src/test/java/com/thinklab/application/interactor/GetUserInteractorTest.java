package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.query.GetUserQuery;
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
import static org.mockito.Mockito.*;

/**
 * Application Unit Test: Validates the retrieval logic of the {@link GetUserInteractor}.
 * This suite ensures that the identity lookup correctly enforces organizational
 * isolation and maps empty results to standardized business exceptions
 * for RFC 7807 compliance.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: GetUser Interactor")
class GetUserInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    private GetUserInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final GetUserQuery query = new GetUserQuery(tenantId, userId);

    @BeforeEach
    void setUp() {
        interactor = new GetUserInteractor(userRepository);
    }

    @Test
    @DisplayName("Should successfully retrieve user metadata when identity exists")
    void shouldReturnUserWhenFound() {
        // Given: A valid identity in the domain
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var userMock = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin");

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.just(userMock));

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.username().equals("john.doe") &&
                        user.tenantId().equals(tenantId))
                .verifyComplete();

        verify(userRepository, times(1)).findByIdAndTenantId(userId, tenantId);
    }

    @Test
    @DisplayName("Should emit UserNotFoundException when identity is missing in context")
    void shouldThrowExceptionWhenNotFound() {
        // Given: Repository signal for non-existent resource
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Mono.empty());

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, times(1)).findByIdAndTenantId(userId, tenantId);
    }

    @Test
    @DisplayName("Should fail fast with NullPointerException if query is null")
    void shouldFailFastWhenQueryIsNull() {
        assertThrows(NullPointerException.class, () -> interactor.execute(null),
                "Interactor must prevent execution of null intent objects.");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should propagate infrastructure failure and log telemetry")
    void shouldPropagateInfrastructureError() {
        // Given: A critical persistence failure
        when(userRepository.findByIdAndTenantId(userId, tenantId))
                .thenReturn(Mono.error(new RuntimeException("MongoDB Connection Timeout")));

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Connection Timeout")
                .verify();
    }
}