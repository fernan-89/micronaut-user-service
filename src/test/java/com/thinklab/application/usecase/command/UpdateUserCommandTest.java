package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command Unit Test: Validates the boundary defense and input integrity
 * of the {@link UpdateUserCommand}.
 */
@DisplayName("Application: UpdateUser Command")
class UpdateUserCommandTest {

    private final UUID userId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile(
            "John Doe",
            "john.doe@thinklab.com",
            "+5511999998888",
            "pt-br",
            "America/Sao_Paulo"
    );

    @Test
    @DisplayName("Should successfully instantiate command with valid data and sanitization")
    void shouldCreateValidCommand() {
        // Given
        String rawExecutor = "  admin-agent-01  ";

        // When
        var command = new UpdateUserCommand(userId, profile, rawExecutor);

        // Then
        assertAll("Command Integrity",
                () -> assertEquals(userId, command.userId()),
                () -> assertEquals(profile, command.profile()),
                () -> assertEquals("admin-agent-01", command.executor(), "Executor ID should be trimmed")
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields (Defense in Depth)")
    void shouldFailOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserCommand(null, profile, "admin"), "userId is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserCommand(userId, null, "admin"), "profile metadata is mandatory"),
                () -> assertThrows(NullPointerException.class, () ->
                        new UpdateUserCommand(userId, profile, null), "executor identification is mandatory")
        );
    }

    @Test
    @DisplayName("Should fail fast if executor identification is blank after trimming")
    void shouldFailOnBlankExecutor() {
        assertThrows(IllegalArgumentException.class, () ->
                        new UpdateUserCommand(userId, profile, "   "),
                "Command must reject empty executor strings to preserve audit quality.");
    }
}