package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.UpdateUserCommand;
import com.thinklab.domain.valueobject.UserProfile;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Infrastructure DTO: Represents the HTTP request payload for updating an existing
 * user's biographical profile.
 * This record enforces strict boundary validation and ensures that forensic
 * authorization metadata is captured at the edge, preventing malformed
 * or anonymous mutations.
 */
@Serdeable
@Introspected
@Schema(
        name = "UpdateUserRequest",
        description = "Payload required to modify the biographical metadata of an existing Enterprise Identity."
)
public record UpdateUserRequest(
        @Valid
        @NotNull(message = "Updated profile metadata is mandatory")
        @Schema(description = "The new biographical and localization state for the identity")
        UserProfile profile,

        @NotBlank(message = "Executor identification is mandatory for forensic auditing")
        @Size(max = 100, message = "Executor identifier is too long")
        @Schema(description = "The agent (admin or service) authorizing this mutation", example = "iam-admin-02")
        String executor
) {

    /**
     * Transformation: Maps the inbound infrastructure request to a pure
     * application-level command.
     * It integrates the target resource identifier from the transport path
     * with the biographical payload from the body.
     *
     * @param userId The unique system identifier of the target user (from Path Variable).
     * @return A validated {@link UpdateUserCommand} ready for orchestration.
     */
    public UpdateUserCommand toCommand(UUID userId) {
        return new UpdateUserCommand(
                userId,
                this.profile,
                this.executor
        );
    }
}