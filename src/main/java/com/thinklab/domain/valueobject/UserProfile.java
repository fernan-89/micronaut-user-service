package com.thinklab.domain.valueobject;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * Domain Value Object: Encapsulates the biographical and contact identification
 * metadata for an Enterprise Identity (User).
 * This record ensures that personal identity data is sanitized and immutable,
 * strictly separating biographical concerns from security credentials.
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Immutability:</b> Uses Java Record to ensure thread-safety in reactive pipelines.</li>
 *     <li><b>Sanitization:</b> Normalizes inputs (trim/lowercase) to prevent index fragmentation.</li>
 *     <li><b>Defensive Integrity:</b> Validates mandatory identity invariants at construction time.</li>
 * </ul>
 *
 * @param fullName          The user's complete legal name (Mandatory).
 * @param corporateEmail    Primary business communication channel (Mandatory/Unique).
 * @param phoneNumber       Formatted contact number (Optional).
 * @param preferredLanguage ISO 639-1 language code for localization (Default: "en").
 * @param timezone          IANA timezone identifier (Default: "UTC").
 */
@Serdeable
@Introspected
public record UserProfile(
        @Nonnull String fullName,
        @Nonnull String corporateEmail,
        @Nullable String phoneNumber,
        @Nonnull String preferredLanguage,
        @Nonnull String timezone
) {

    /**
     * Compact constructor for input sanitization and domain invariant protection.
     * Enforces that identity boundaries are never violated by malformed data.
     */
    public UserProfile {
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(corporateEmail, "corporateEmail cannot be null");

        // Input Sanitization
        fullName = fullName.trim();
        corporateEmail = corporateEmail.trim().toLowerCase();
        phoneNumber = phoneNumber != null ? phoneNumber.trim() : null;
        preferredLanguage = preferredLanguage != null ? preferredLanguage.trim().toLowerCase() : "en";
        timezone = timezone != null ? timezone.trim() : "UTC";

        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is mandatory for identity profiling.");
        }

        if (corporateEmail.isBlank() || !corporateEmail.contains("@")) {
            throw new IllegalArgumentException("A valid corporate email is required.");
        }
    }
}