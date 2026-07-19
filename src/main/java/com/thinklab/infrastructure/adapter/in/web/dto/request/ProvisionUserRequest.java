package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.ProvisionUserCommand;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Infrastructure DTO: Represents the HTTP request payload for provisioning a new
 * Enterprise Identity (User).
 * This record acts as the system's entry boundary, enforcing strict syntactic
 * validation using Jakarta Bean Validation and providing metadata for
 * automated OpenAPI documentation.
 */
@Serdeable
@Introspected
@Schema(
        name = "ProvisionUserRequest",
        description = "Payload required to initiate the provisioning of a new user identity within a tenant context."
)
public record ProvisionUserRequest(
        @NotNull(message = "Tenant identifier is mandatory")
        @Schema(description = "The isolated organizational context ID", example = "7c6b0035-2033-4ac5-9658-9060fd1f7a1e")
        UUID tenantId,

        @Nullable
        @Schema(description = "Optional parent unit ID for hierarchical associations", example = "af9c2b03-384d-3a6d-8465-64ba383d3bad")
        UUID parentId,

        @NotBlank(message = "Username identifier is mandatory")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Schema(description = "Unique system login name", example = "john.doe")
        String username,

        @NotBlank(message = "Corporate email is mandatory")
        @Email(message = "Invalid email format")
        @Schema(description = "Primary corporate contact email", example = "john.doe@thinklab.com")
        String email,

        @NotNull(message = "User level is mandatory")
        @Schema(description = "Hierarchical level defining global authority")
        UserLevel level,

        @Valid
        @NotNull(message = "User profile metadata is mandatory")
        @Schema(description = "Biographical and localization metadata")
        UserProfile profile,

        @NotBlank(message = "Executor identification is mandatory")
        @Schema(description = "The agent (admin or service) authorizing this action", example = "iam-admin-01")
        String executor
) {

    /**
     * Transformation: Maps the infrastructure-bound request into a pure
     * application-bound command.
     * Ensures that the business logic layer remains shielded from transport concerns.
     */
    public ProvisionUserCommand toCommand() {
        return new ProvisionUserCommand(
                this.tenantId,
                this.parentId,
                this.username,
                this.email,
                this.level,
                this.profile,
                this.executor
        );
    }
}