package com.thinklab.application.usecase.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command Unit Test: Validates the boundary defense and input integrity
 * of the {@link UpdateUserRolesCommand}.
 *
 * <p>Following the NASA-level engineering blueprint, this suite ensures
 * that security-critical role mutations are sanitized and strictly validated
 * at the system boundary to prevent unauthorized or malformed state changes.</p>
 */
@DisplayName("Application: UpdateUserRoles Command")
class UpdateUserRolesCommandTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final List<String> roles = List.of("ROLE_ADMIN", "REPORTS_VIEWER");

    @Test
    @DisplayName("Should successfully instantiate command with valid data and sanitization")
    void shouldCreateValidCommand() {
        // Given
        String rawExecutor = "  security-admin-01  ";

        // When
        var command = new UpdateUserRolesCommand(tenantId, userId, roles, rawExecutor);

        // Then
        assertAll("Command Integrity",
                () -> assertEquals(tenantId, command.tenantId()),
                () -> assertEquals(userId, command.userId()),
                () -> assertEquals(roles, command.roles()),
                () -> assertEquals("security-admin-01", command.executor(), "Executor ID should be trimmed")
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields (Defense in Depth)")
    void shouldFailOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(null, userId, roles, "admin"), "tenantId is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(tenantId, null, roles, "admin"), "userId is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(tenantId, userId, null, "admin"), "roles list is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserRolesCommand(tenantId, userId, roles, null), "executor is mandatory")
        );
    }

    @Test
    @DisplayName("Should fail fast if executor identification is blank after trimming")
    void shouldFailOnBlankExecutor() {
        assertThrows(IllegalArgumentException.class, () ->
                        new UpdateUserRolesCommand(tenantId, userId, roles, "   "),
                "Command must reject empty executor strings to preserve audit quality.");
    }

    @Test
    @DisplayName("Should fail fast if roles list is empty")
    void shouldFailOnEmptyRoles() {
        assertThrows(IllegalArgumentException.class, () ->
                        new UpdateUserRolesCommand(tenantId, userId, List.of(), "admin"),
                "Command should reject empty role assignments to prevent privilege stripping without intent.");
    }
}