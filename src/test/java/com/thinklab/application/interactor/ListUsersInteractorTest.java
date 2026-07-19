package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.query.ListUsersQuery;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Application Unit Test: Validates the retrieval orchestration of the {@link ListUsersInteractor}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application: ListUsers Interactor")
class ListUsersInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    private ListUsersInteractor interactor;

    private final UUID tenantId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile("Test", "User", "test@thinklab.com", "123456", "Doc123");
    private final User userMock = User.provision(tenantId, null, "test.user", UserLevel.OPERATOR, profile, "admin");

    @BeforeEach
    void setUp() {
        interactor = new ListUsersInteractor(userRepository);
    }

    @Test
    @DisplayName("Should route to status-filtered repository method when status is provided in query")
    void shouldListWithStatusFilter() {
        // Given: A query containing a specific status filter
        // Passando 'null' para UserLevel se não quiser filtrar por nível, ou especifique um
        var query = new ListUsersQuery(tenantId, UserStatus.ACTIVE, null, 0, 10);

        when(userRepository.findByTenantIdAndStatus(eq(tenantId), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(Flux.just(userMock));

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectNext(userMock)
                .verifyComplete();

        verify(userRepository, times(1)).findByTenantIdAndStatus(eq(tenantId), eq(UserStatus.ACTIVE), any(Pageable.class));
        verify(userRepository, never()).findByTenantId(any(), any());
    }

    @Test
    @DisplayName("Should route to generic tenant listing when status filter is absent")
    void shouldListWithoutStatusFilter() {
        // Given
        var query = new ListUsersQuery(tenantId, null, null, 0, 10);

        when(userRepository.findByTenantId(eq(tenantId), any(Pageable.class)))
                .thenReturn(Flux.just(userMock));

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectNext(userMock)
                .verifyComplete();

        verify(userRepository, times(1)).findByTenantId(eq(tenantId), any(Pageable.class));
        verify(userRepository, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should emit empty flux signal when no identities match the discovery criteria")
    void shouldReturnEmptyFlux() {
        // Given
        var query = new ListUsersQuery(tenantId, null, null, 0, 10);

        when(userRepository.findByTenantId(eq(tenantId), any(Pageable.class)))
                .thenReturn(Flux.empty());

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail fast with NullPointerException if query intent is missing")
    void shouldFailFastOnNullQuery() {
        assertThrows(NullPointerException.class, () -> interactor.execute(null),
                "Interactor must prevent construction of streams for null intents.");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should propagate infrastructure exceptions through the reactive pipeline")
    void shouldPropagateRepositoryError() {
        // Given
        var query = new ListUsersQuery(tenantId, null, null, 0, 10);

        when(userRepository.findByTenantId(eq(tenantId), any(Pageable.class)))
                .thenReturn(Flux.error(new RuntimeException("MongoDB Cursor Exhausted")));

        // When
        var result = interactor.execute(query);

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("MongoDB Cursor Exhausted")
                .verify();
    }
}