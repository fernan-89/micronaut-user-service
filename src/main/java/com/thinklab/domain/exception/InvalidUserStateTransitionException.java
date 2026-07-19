package com.thinklab.domain.exception;

import com.thinklab.domain.valueobject.UserStatus;
import jakarta.annotation.Nonnull;

/**
 * Domain Business Exception: Signaling an illegal attempt to transition a user
 * between operational states.
 *
 * <p>This exception is the primary defense mechanism of the User State Machine (FSM).
 * It ensures that identity lifecycles cannot be corrupted by invalid business
 * logic or unauthorized administrative actions.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Standardized Mapping:</b> Uses "INVALID_STATE_TRANSITION" for RFC 7807 compliance.</li>
 *     <li><b>Diagnostic Richness:</b> Provides clear context of why the transition was denied.</li>
 * </ul>
 */
public class InvalidUserStateTransitionException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_STATE_TRANSITION";

    /**
     * Semantic constructor: Formats a detailed message describing the illegal transition.
     *
     * @param current The current lifecycle status of the user.
     * @param target  The illegal intended destination status.
     */
    public InvalidUserStateTransitionException(@Nonnull UserStatus current, @Nonnull UserStatus target) {
        super(ERROR_CODE, String.format(
                "Illegal state transition detected: Identity is currently [%s] and cannot be transitioned to [%s].",
                current, target));
    }

    /**
     * Fallback constructor for general state violation messages.
     *
     * @param message Detailed explanation of the compliance violation.
     */
    public InvalidUserStateTransitionException(String message) {
        super(ERROR_CODE, message);
    }
}