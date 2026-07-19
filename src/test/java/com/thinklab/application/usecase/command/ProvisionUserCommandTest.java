package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command Unit Test: Validates the boundary defense and input sanitization
 * of the {@link ProvisionUserCommand}.
 *
 * <p>This suite ensures that the command, acting as the primary input port,
 * strictly enforces nullability contracts and sanitizes biographical data
 * to prevent domain contamination.</p>
 */
@DisplayName("Application: ProvisionUser Command")
class ProvisionUserCommandTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile("John Doe", "john@thinklab.com", null, "en", "UTC");

    @Test
    @DisplayName("Should successfully instantiate command with valid data and sanitization")
    void shouldCreateValidCommand() {
        // Given
        String rawUsername = "  john.doe  ";
        String rawEmail = " JOHN.DOE@THINKLAB.COM ";

        // When
        var command = new ProvisionUserCommand(
                tenantId,
                null,
                rawUsername,
                rawEmail,
                UserLevel.OPERATOR,
                profile,
                "admin-01"
        );

        // Then
        assertAll("Command Integrity",
                () -> assertEquals(tenantId, command.tenantId()),
                () -> assertEquals("john.doe", command.username(), "Username should be trimmed"),
                () -> assertEquals("john.doe@thinklab.com", command.email(), "Email should be trimmed and lowercased"),
                () -> assertEquals(UserLevel.OPERATOR, command.level()),
                () -> assertEquals("admin-01", command.executor())
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields (Defense in Depth)")
    void shouldFailOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(null, null, "user", "a@b.com", UserLevel.OPERATOR, profile, "admin")),
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(tenantId, null, null, "a@b.com", UserLevel.OPERATOR, profile, "admin")),
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "user", null, UserLevel.OPERATOR, profile, "admin")),
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "user", "a@b.com", null, profile, "admin")),
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "user", "a@b.com", UserLevel.OPERATOR, null, "admin")),
                () -> assertThrows(NullPointerException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "user", "a@b.com", UserLevel.OPERATOR, profile, null))
        );
    }

    @Test
    @DisplayName("Should fail fast if username or email are blank after trimming")
    void shouldFailOnBlankFields() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "   ", "a@b.com", UserLevel.OPERATOR, profile, "admin")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ProvisionUserCommand(tenantId, null, "user", "  ", UserLevel.OPERATOR, profile, "admin"))
        );
    }

    @Test
    @DisplayName("Should preserve parentId when provided (Branch Provisioning)")
    void shouldPreserveParentId() {
        // Given
        UUID parentId = UUID.randomUUID();

        // When
        var command = new ProvisionUserCommand(tenantId, parentId, "user", "a@b.com", UserLevel.OPERATOR, profile, "admin");

        // Then
        assertEquals(parentId, command.parentId());
    }
}