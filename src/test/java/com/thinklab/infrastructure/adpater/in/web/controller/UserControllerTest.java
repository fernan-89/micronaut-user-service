package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.port.in.*;
import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.request.ProvisionUserRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.request.UpdateUserStatusRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.response.UserResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Infrastructure Test: Validates the {@link UserController} routing, serialization,
 * and boundary defense.
 */
@MicronautTest(transactional = false)
@Property(name = "mongodb.uri", value = "mongodb://localhost:27017/unit_test")
@DisplayName("Infrastructure: User Web Controller")
class UserControllerTest {

    @Inject
    UserController userController;

    @Inject
    ProvisionUserUseCase provisionUseCase;

    @Inject
    GetUserUseCase getUserUseCase;

    @Inject
    UpdateUserStatusUseCase statusUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Should return 201 CREATED when provisioning a valid identity")
    void shouldReturn201OnProvision() {
        // Given
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var request = new ProvisionUserRequest(tenantId, null, "john.doe", "john@thinklab.com", UserLevel.OPERATOR, profile, "admin");

        var userMock = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin");
        when(provisionUseCase.execute(any())).thenReturn(Mono.just(userMock));

        // When
        Mono<HttpResponse<UserResponse>> result = userController.provision(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatus());
                    assertNotNull(response.body());
                    assertEquals("john.doe", response.body().username());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 OK when retrieving an existing user")
    void shouldReturn200OnGet() {
        // Given
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");
        var userMock = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin");
        when(getUserUseCase.execute(any())).thenReturn(Mono.just(userMock));

        // When
        Mono<HttpResponse<UserResponse>> result = userController.getById(tenantId, userId);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                    assertEquals(tenantId, response.body().tenantId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 OK on successful lifecycle state transition")
    void shouldReturn200OnStatusUpdate() {
        // Given
        var request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, "admin", "Temporary suspension");
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");

        // CORREÇÃO: Não tentamos chamar .suspend() no objeto User.
        // Apenas criamos o mock do user no estado desejado para o teste de interface.
        var suspendedUser = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin");
        // Se precisar representar o estado suspenso, faça-o via configuração do mock do UseCase

        when(statusUseCase.execute(any())).thenReturn(Mono.just(suspendedUser));

        // When
        Mono<HttpResponse<UserResponse>> result = userController.updateStatus(userId, request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    // Verifica se o DTO de resposta está sendo montado corretamente
                    assertNotNull(response.body());
                })
                .verifyComplete();
    }

    // Mocks definidos corretamente
    @jakarta.inject.Singleton
    @io.micronaut.context.annotation.Replaces(ProvisionUserUseCase.class)
    ProvisionUserUseCase provisionUserUseCase() { return Mockito.mock(ProvisionUserUseCase.class); }

    @jakarta.inject.Singleton
    @io.micronaut.context.annotation.Replaces(GetUserUseCase.class)
    GetUserUseCase getUserUseCase() { return Mockito.mock(GetUserUseCase.class); }

    @jakarta.inject.Singleton
    @io.micronaut.context.annotation.Replaces(UpdateUserStatusUseCase.class)
    UpdateUserStatusUseCase updateUserStatusUseCase() { return Mockito.mock(UpdateUserStatusUseCase.class); }

    @jakarta.inject.Singleton
    @io.micronaut.context.annotation.Replaces(UpdateUserRolesUseCase.class)
    UpdateUserRolesUseCase updateUserRolesUseCase() { return Mockito.mock(UpdateUserRolesUseCase.class); }

    @jakarta.inject.Singleton
    @io.micronaut.context.annotation.Replaces(ListUsersUseCase.class)
    ListUsersUseCase listUsersUseCase() { return Mockito.mock(ListUsersUseCase.class); }
}