package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command Unit Test: Validates the boundary defense and input integrity
 * of the {@link UpdateUserStatusCommand}.
 *
 * <p>Following the NASA-level engineering blueprint, this suite ensures that
 * lifecycle state transitions are strictly validated at the system boundary,
 * requiring mandatory forensic justification and high-fidelity identification
 * of the authorizing agent.</p>
 */
@DisplayName("Application: UpdateUserStatus Command")
class UpdateUserStatusCommandTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UserStatus targetStatus = UserStatus.SUSPENDED;

    @Test
    @DisplayName("Should successfully instantiate command with valid data and sanitization")
    void shouldCreateValidCommand() {
        // Given
        String rawExecutor = "  compliance-officer-01  ";
        String rawReason = "  Suspension due to suspected credential compromise.  ";

        // When
        var command = new UpdateUserStatusCommand(tenantId, userId, targetStatus, rawExecutor, rawReason);

        // Then
        assertAll("Command Integrity",
                () -> assertEquals(tenantId, command.tenantId()),
                () -> assertEquals(userId, command.userId()),
                () -> assertEquals(targetStatus, command.status()),
                () -> assertEquals("compliance-officer-01", command.executor(), "Executor ID should be trimmed"),
                () -> assertEquals("Suspension due to suspected credential compromise.", command.reason(), "Reason should be trimmed")
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields (Defense in Depth)")
    void shouldFailOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserStatusCommand(null, userId, targetStatus, "admin", "reason"), "tenantId is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserStatusCommand(tenantId, null, targetStatus, "admin", "reason"), "userId is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserStatusCommand(tenantId, userId, null, "admin", "reason"), "target status is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserStatusCommand(tenantId, userId, targetStatus, null, "reason"), "executor is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserStatusCommand(tenantId, userId, targetStatus, "admin", null), "reason is mandatory")
        );
    }

    @Test
    @DisplayName("Should fail fast if executor or reason are blank after trimming")
    void shouldFailOnBlankFields() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                                new UpdateUserStatusCommand(tenantId, userId, targetStatus, "   ", "valid reason"),
                        "Command must reject empty executor strings"),
                () -> assertThrows(IllegalArgumentException.class, () ->
                                new UpdateUserStatusCommand(tenantId, userId, targetStatus, "admin", "   "),
                        "Command must reject empty justification for lifecycle changes")
        );
    }

    @Test
    @DisplayName("Should maintain state immutability through reactive pipeline")
    void shouldBeImmutable() {
        var command = new UpdateUserStatusCommand(tenantId, userId, targetStatus, "admin", "reason");
        // Records are inherently immutable and final.
        assertTrue(command.getClass().isRecord());
    }
}