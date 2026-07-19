package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.UpdateUserStatusCommand;
import com.thinklab.domain.valueobject.UserStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Infrastructure DTO: Represents the HTTP request payload for modifying a user's
 * operational lifecycle status.
 * This record enforces strict boundary validation, ensuring that state transitions
 * are accompanied by mandatory forensic metadata and authorization context.
 */
@Serdeable
@Introspected
@Schema(
        name = "UpdateUserStatusRequest",
        description = "Payload required to transition an identity between operational states (e.g., ACTIVE to SUSPENDED)."
)
public record UpdateUserStatusRequest(
        @NotNull(message = "Target status is mandatory")
        @Schema(description = "The intended new state for the identity")
        UserStatus status,

        @NotBlank(message = "Executor identification is mandatory for forensic auditing")
        @Size(max = 100, message = "Executor identifier is too long")
        @Schema(description = "The agent (admin or service) authorizing this state change", example = "security-officer-01")
        String executor,

        @NotBlank(message = "Business justification is mandatory for status transitions")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        @Schema(description = "Detailed justification for the lifecycle transition", example = "Excessive failed login attempts detected.")
        String reason
) {

    /**
     * Transformation: Maps the inbound infrastructure request into a pure
     * application-level command.
     * Integrates the target resource identifier with the state transition payload.
     *
     * @param userId The unique system identifier of the target user (from Path Variable).
     * @return A validated {@link UpdateUserStatusCommand} ready for orchestration.
     */
    public UpdateUserStatusCommand toCommand(UUID userId) {
        return new UpdateUserStatusCommand(
                userId,
                this.status,
                this.executor,
                this.reason
        );
    }
}