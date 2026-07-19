package com.thinklab.domain.exception;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * Domain Exception: Base class for all organizational and identity business rule violations.
 * This exception serves as the primary boundary signaling mechanism, differentiating
 * logical domain errors (e.g., state violations, duplicate identities) from
 * technological infrastructure failures (e.g., database timeouts).
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Pure Java:</b> Zero dependencies on web frameworks or persistence libraries.</li>
 *     <li><b>Error Categorization:</b> Mandatory error codes for predictable client-side handling.</li>
 *     <li><b>Chaining Support:</b> Preserves the original root cause for forensic analysis.</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a Business Exception with a standardized code and descriptive message.
     * Implements defensive fallback to prevent NullPointerExceptions during instantiation.
     *
     * @param errorCode A standardized string code (e.g., "USER_NOT_FOUND") for API categorization.
     * @param message   A human-readable explanation of the specific violation.
     */
    public BusinessException(@Nullable String errorCode, @Nullable String message) {
        super(message != null ? message : "A domain business rule was violated.");
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_BUSINESS_ERROR";
    }

    /**
     * Constructs a Business Exception preserving the original underlying cause.
     *
     * @param errorCode Standardized business error code.
     * @param message   Descriptive violation message.
     * @param cause     The underlying exception that triggered this violation.
     */
    public BusinessException(@Nullable String errorCode, @Nullable String message, @Nullable Throwable cause) {
        super(message != null ? message : "A domain business rule was violated.", cause);
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_BUSINESS_ERROR";
    }

    /**
     * Retrieves the standardized error code associated with this domain violation.
     * This code is intended to be used by infrastructure adapters for protocol mapping (e.g., RFC 7807).
     *
     * @return The immutable error code string.
     */
    @Nonnull
    public String getErrorCode() {
        return errorCode;
    }
}