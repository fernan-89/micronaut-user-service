package com.thinklab.domain.exception;

import jakarta.annotation.Nonnull;
import java.util.UUID;

/**
 * Domain Business Exception: Signaling that a requested Enterprise Identity
 * could not be located within the organizational context.
 *
 * <p>This specific exception extends {@link BusinessException} to enforce
 * the standardized "USER_NOT_FOUND" error code. This ensures that infrastructure
 * adapters correctly project the failure as a compliant HTTP 404 (Not Found)
 * response, maintaining strict boundary protocols.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Semantic Construction:</b> Encourages providing the target ID for better diagnostics.</li>
 *     <li><b>Standardized Mapping:</b> Guaranteed to be intercepted by the Global Resilience Layer.</li>
 * </ul>
 */
public class UserNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "USER_NOT_FOUND";

    /**
     * Semantic constructor: Formats a standardized message based on the missing User ID.
     *
     * @param userId The unique identifier of the target user that was not found.
     */
    public UserNotFoundException(@Nonnull UUID userId) {
        super(ERROR_CODE, String.format("Enterprise Identity with identifier [%s] was not found in the registry.", userId));
    }

    /**
     * Fallback constructor for custom error messages.
     *
     * @param message Detailed human-readable explanation of the retrieval failure.
     */
    public UserNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}