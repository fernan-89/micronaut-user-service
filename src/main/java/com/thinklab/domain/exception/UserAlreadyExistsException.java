package com.thinklab.domain.exception;

import jakarta.annotation.Nonnull;

/**
 * Domain Business Exception: Signaling that a provisioning attempt failed because
 * the identity (Username or Email) is already registered within the organizational context.
 *
 * <p>This specific exception extends {@link BusinessException} to enforce
 * the standardized "USER_ALREADY_EXISTS" error code. This ensures that infrastructure
 * adapters correctly project the failure as a compliant HTTP 409 (Conflict)
 * response, maintaining strict boundary protocols.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Semantic Integrity:</b> Clearly identifies the specific field causing the collision.</li>
 *     <li><b>Standardized Mapping:</b> Guaranteed to be intercepted by the Global Resilience Layer.</li>
 * </ul>
 */
public class UserAlreadyExistsException extends BusinessException {

    private static final String ERROR_CODE = "USER_ALREADY_EXISTS";

    /**
     * Semantic constructor: Formats a standardized message identifying the conflicting attribute.
     *
     * @param field The attribute name causing the conflict (e.g., "email", "username").
     * @param value The value that already exists in the identity registry.
     */
    public UserAlreadyExistsException(@Nonnull String field, @Nonnull String value) {
        super(ERROR_CODE, String.format(
                "Identity collision detected: A user with %s [%s] is already established in this organizational context.",
                field, value));
    }

    /**
     * Fallback constructor for custom or general collision messages.
     *
     * @param message Detailed human-readable explanation of the duplication failure.
     */
    public UserAlreadyExistsException(String message) {
        super(ERROR_CODE, message);
    }
}