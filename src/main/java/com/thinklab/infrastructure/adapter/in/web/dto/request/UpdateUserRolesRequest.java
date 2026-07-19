package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.UpdateUserRolesCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Infrastructure DTO: Represents the HTTP request payload for modifying a user's
 * access matrix and functional roles.
 * This record enforces strict boundary validation, mandating that all privilege
 * mutations are accompanied by forensic metadata and business justifications
 * required for security compliance.
 */
@Serdeable
@Introspected
@Schema(
        name = "UpdateUserRolesRequest",
        description = "Payload required to modify the collection of functional roles assigned to an identity."
)
public record UpdateUserRolesRequest(
        @NotEmpty(message = "Roles collection cannot be empty")
        @Schema(description = "The new complete set of functional roles for the user", example = "[\"OPERATOR\", \"REPORTS_VIEWER\"]")
        List<String> roles,

        @NotBlank(message = "Executor identification is mandatory for forensic auditing")
        @Size(max = 100, message = "Executor identifier is too long")
        @Schema(description = "The agent (admin or service) authorizing this authorization change", example = "security-admin-01")
        String executor,

        @NotBlank(message = "Business justification is mandatory for privilege mutations")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        @Schema(description = "Detailed justification for the change in access rights", example = "Promotion to regional supervisor unit.")
        String reason
) {

    /**
     * Transformation: Maps the inbound infrastructure request into a pure
     * application-level command.
     * Consolidates the target resource identifier with the security payload.
     *
     * @param userId The unique system identifier of the target user (from Path Variable).
     * @return A validated {@link UpdateUserRolesCommand} ready for orchestration.
     */
    public UpdateUserRolesCommand toCommand(@NotNull UUID userId) {
        return new UpdateUserRolesCommand(
                userId,
                List.copyOf(this.roles),
                this.executor,
                this.reason
        );
    }
}