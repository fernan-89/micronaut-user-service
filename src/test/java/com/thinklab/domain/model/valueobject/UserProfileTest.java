package com.thinklab.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain Unit Test: Validates the biographical integrity and sanitization
 * logic of the {@link UserProfile} value object.
 */
@DisplayName("Domain: UserProfile Value Object")
class UserProfileTest {

    @Test
    @DisplayName("Should successfully create profile with data sanitization")
    void shouldCreateProfileWithSanitization() {
        // Given
        String rawName = "  John Doe  ";
        String rawEmail = " JOHN.DOE@THINKLAB.COM  ";
        String rawPhone = " +5511999999999 ";

        // When
        var profile = new UserProfile(rawName, rawEmail, rawPhone, " PT-BR ", " America/Sao_Paulo ");

        // Then
        assertEquals("John Doe", profile.fullName(), "Full name should be trimmed");
        assertEquals("john.doe@thinklab.com", profile.corporateEmail(), "Email should be trimmed and lowercased");
        assertEquals("+5511999999999", profile.phoneNumber(), "Phone number should be trimmed");
        assertEquals("pt-br", profile.preferredLanguage(), "Language should be lowercased");
        assertEquals("America/Sao_Paulo", profile.timezone(), "Timezone should be trimmed");
    }

    @Test
    @DisplayName("Should apply default values for optional localization fields")
    void shouldApplyDefaultsForOptionalFields() {
        // Given & When
        var profile = new UserProfile("John Doe", "john@thinklab.com", null, null, null);

        // Then
        assertNull(profile.phoneNumber());
        // Ajuste estes assertEquals conforme a implementação do seu construtor/factory
        assertEquals("en", profile.preferredLanguage(), "Should fallback to English");
        assertEquals("UTC", profile.timezone(), "Should fallback to UTC");
    }

    @Test
    @DisplayName("Should throw exception when full name is blank")
    void shouldThrowExceptionWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserProfile("   ", "john@thinklab.com", null, "en", "UTC"),
                "Should fail fast on empty identity name");
    }

    @Test
    @DisplayName("Should throw exception when email is malformed")
    void shouldThrowExceptionWhenEmailIsMalformed() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserProfile("John Doe", "invalid-email", null, "en", "UTC"),
                "Should fail fast on invalid email format");
    }

    @Test
    @DisplayName("Should throw NullPointerException for mandatory fields")
    void shouldThrowNpeOnNullMandatoryFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new UserProfile(null, "j@t.com", null, "en", "UTC")),
                () -> assertThrows(NullPointerException.class, () -> new UserProfile("John", null, null, "en", "UTC"))
        );
    }
}