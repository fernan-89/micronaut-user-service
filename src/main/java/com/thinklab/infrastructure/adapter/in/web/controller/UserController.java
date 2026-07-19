package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.port.in.*;
import com.thinklab.application.usecase.query.GetUserQuery;
import com.thinklab.application.usecase.query.ListUsersQuery;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.request.*;
import com.thinklab.infrastructure.adapter.in.web.dto.response.PagedUserResponse;
import com.thinklab.infrastructure.adapter.in.web.dto.response.UserResponse;
import com.thinklab.infrastructure.adapter.out.mongo.mapper.UserMapper;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST Controller: The primary inbound adapter for Enterprise Identity management.
 * Acts as a high-performance, non-blocking mediation layer between the HTTP protocol
 * and Application Use Cases. It enforces boundary defense, tenant isolation,
 * and secure data projection.
 */
@Slf4j
@Controller("/users")
@Tag(name = "Identity Management API", description = "Endpoints for orchestrating user provisioning, lifecycle transitions, and hierarchical discovery.")
public class UserController {

    private final ProvisionUserUseCase provisionUseCase;
    private final UpdateUserUseCase updateUseCase;
    private final UpdateUserStatusUseCase statusUseCase;
    private final UpdateUserRolesUseCase rolesUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UserMapper mapper;

    @Inject
    public UserController(ProvisionUserUseCase provisionUseCase,
                          UpdateUserUseCase updateUseCase,
                          UpdateUserStatusUseCase statusUseCase,
                          UpdateUserRolesUseCase rolesUseCase,
                          GetUserUseCase getUserUseCase,
                          ListUsersUseCase listUsersUseCase,
                          UserMapper mapper) {
        this.provisionUseCase = provisionUseCase;
        this.updateUseCase = updateUseCase;
        this.statusUseCase = statusUseCase;
        this.rolesUseCase = rolesUseCase;
        this.getUserUseCase = getUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.mapper = mapper;
    }

    @Post
    @Operation(summary = "Provision user", description = "Establishes a new identity with mandatory forensic auditing and tenant isolation.")
    @ApiResponse(responseCode = "201", description = "Identity provisioned and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid payload or validation failure.")
    public Mono<HttpResponse<UserResponse>> provision(@Body @Valid ProvisionUserRequest request) {
        return provisionUseCase.execute(request.toCommand())
                .map(mapper::toResponse)
                .map(body -> (HttpResponse<UserResponse>) HttpResponse.created(body))
                .doOnSubscribe(s -> log.info("[ACTION: PROVISION_USER] [TENANT: {}] - Initiating provisioning protocol.", request.tenantId()))
                .doOnSuccess(res -> log.info("[ACTION: PROVISION_USER] [ID: {}] - Identity established status: 201.", res.body().id()));
    }

    @Get("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves current metadata and state for a specific identity within a tenant context.")
    @ApiResponse(responseCode = "200", description = "Identity located and projected.")
    @ApiResponse(responseCode = "404", description = "Identity not found in this organizational context.")
    public Mono<HttpResponse<UserResponse>> getById(
            @Header("X-Tenant-Id") @Nonnull UUID tenantId,
            @PathVariable @Parameter(description = "The UUID of the target user") UUID id) {
        return getUserUseCase.execute(new GetUserQuery(tenantId, id))
                .map(mapper::toResponse)
                .map(body -> (HttpResponse<UserResponse>) HttpResponse.ok(body))
                .doOnSubscribe(s -> log.info("[ACTION: GET_USER] [ID: {}] - Retrieving identity metadata.", id));
    }

    @Get
    @Operation(summary = "List tenant users", description = "Streams a paginated subset of identities scoped strictly to the organization header.")
    @ApiResponse(responseCode = "200", description = "Paginated stream retrieved successfully.")
    public Mono<HttpResponse<PagedUserResponse>> list(
            @Header("X-Tenant-Id") @Nonnull UUID tenantId,
            @QueryValue @Nullable UserStatus status,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "20") int size) {

        var query = new ListUsersQuery(tenantId, status, null, page, size);

        return listUsersUseCase.execute(query)
                .map(mapper::toResponse)
                .collectList()
                .map(content -> PagedUserResponse.of(content, 0L, page, size))
                .map(body -> (HttpResponse<PagedUserResponse>) HttpResponse.ok(body))
                .doOnSubscribe(s -> log.info("[ACTION: LIST_USERS] [TENANT: {}] [PAGE: {}] - Discovering identities.", tenantId, page));
    }

    @Put("/{id}")
    @Operation(summary = "Update biographical profile", description = "Mutates non-security biographical metadata. Fully audited operation.")
    @ApiResponse(responseCode = "200", description = "Metadata successfully synchronized.")
    public Mono<HttpResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Body @Valid UpdateUserRequest request) {
        return updateUseCase.execute(request.toCommand(id))
                .map(mapper::toResponse)
                .map(body -> (HttpResponse<UserResponse>) HttpResponse.ok(body))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_USER] [ID: {}] - Initiating metadata mutation.", id));
    }

    @Patch("/{id}/status")
    @Operation(summary = "Transition lifecycle status", description = "Orchestrates critical state machine transitions (e.g., SUSPEND, ACTIVATE). Requires business justification.")
    @ApiResponse(responseCode = "200", description = "State transition committed and audited.")
    public Mono<HttpResponse<UserResponse>> updateStatus(
            @PathVariable UUID id,
            @Body @Valid UpdateUserStatusRequest request) {
        return statusUseCase.execute(request.toCommand(id))
                .map(mapper::toResponse)
                .map(body -> (HttpResponse<UserResponse>) HttpResponse.ok(body))
                .doOnSubscribe(s -> log.info("[ACTION: UPDATE_STATUS] [ID: {}] [TARGET: {}] - Initiating state transition.", id, request.status()));
    }

    @Patch("/{id}/roles")
    @Operation(summary = "Mutate access matrix", description = "Updates functional roles and privileges. High-severity security operation with mandatory auditing.")
    @ApiResponse(responseCode = "200", description = "Authorization matrix successfully synchronized.")
    public Mono<HttpResponse<UserResponse>> updateRoles(
            @PathVariable UUID id,
            @Body @Valid UpdateUserRolesRequest request) {
        return rolesUseCase.execute(request.toCommand(id))
                .map(mapper::toResponse)
                .map(body -> (HttpResponse<UserResponse>) HttpResponse.ok(body))
                .doOnSubscribe(s -> log.warn("[ACTION: UPDATE_ROLES] [ID: {}] - CRITICAL: Initiating privilege mutation.", id));
    }
}