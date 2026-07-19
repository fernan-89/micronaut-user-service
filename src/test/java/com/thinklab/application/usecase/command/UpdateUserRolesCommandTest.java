package com.thinklab.application.usecase.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command Unit Test: Validates the boundary defense of {@link UpdateUserRolesCommand}.
 */
@DisplayName("Application: UpdateUserRoles Command")
class UpdateUserRolesCommandTest {

    private final UUID userId = UUID.randomUUID();
    private final List<String> roles = List.of("ADMIN", "OPERATOR");
    private final String reason = "Forensic compliance update";

    @Test
    @DisplayName("Should successfully instantiate command with valid data and sanitization")
    void shouldCreateValidCommand() {
        // Given
        String rawExecutor = "  security-admin  ";

        // When: Ordem correta: (userId, roles, executor, reason)
        var command = new UpdateUserRolesCommand(userId, roles, rawExecutor, reason);

        // Then
        assertAll("Command Integrity",
                () -> assertEquals(userId, command.userId()),
                () -> assertEquals(roles, command.roles()),
                () -> assertEquals("security-admin", command.executor(), "Executor should be trimmed"),
                () -> assertEquals(reason, command.reason())
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields")
    void shouldFailOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(null, roles, "admin", reason)),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(userId, null, "admin", reason)),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(userId, roles, null, reason)),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(userId, roles, "admin", null))
        );
    }
}