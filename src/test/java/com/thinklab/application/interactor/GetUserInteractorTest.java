package com.thinklab.application.interactor;

import com.thinklab.application.port.out.UserRepositoryPort;
import com.thinklab.application.usecase.query.GetUserQuery;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit Test Suite for {@link GetUserInteractor}.
 * Validates the retrieval logic of User identities while enforcing
 * strict multi-tenant isolation policies.
 */
@ExtendWith(MockitoExtension.class)
class GetUserInteractorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private GetUserInteractor getUserInteractor;

    @Test
    @DisplayName("Should retrieve user when id and tenant context are valid")
    void shouldRetrieveUserWhenValid() {
        // GIVEN: Context setup
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String username = "testuser";

        // Preparando a query encapsulada
        GetUserQuery query = new GetUserQuery(userId, tenantId);

        // Factory Method para instanciar o User de forma limpa
        User userMock = createMockUser(userId, tenantId, username);

        // Mocking behavior
        when(userRepository.findById(userId)).thenReturn(Mono.just(userMock));

        // WHEN: Execution
        Mono<User> result = getUserInteractor.execute(query);

        // THEN: Architectural Verification
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertEquals(userId, user.id());
                    assertEquals(tenantId, user.tenantId());
                    assertEquals(username, user.username());
                })
                .verifyComplete();
    }

    /**
     * Default Factory Method for creating User instances in tests.
     * Encapsulates the 15-parameter constructor complexity.
     */
    private User createMockUser(UUID userId, UUID tenantId, String username) {
        return new User(
                userId,
                tenantId,
                null,               // parentId
                username,
                UserLevel.OPERATOR,
                UserStatus.ACTIVE,
                Collections.emptyList(),
                // Preenchendo os 5 campos exigidos pelo record UserProfile
                new UserProfile("FirstName", "LastName", "Email", "Phone", "Document"),
                0,                  // failedAttempts
                false,              // mfaEnabled
                "system",           // createdBy
                Instant.now(),      // createdAt
                null,               // updatedBy
                null,               // updatedAt
                1L                  // version
        );
    }
}