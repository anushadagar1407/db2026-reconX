package com.dbtraining.reconx.exception;

/**
 * ============================================================================
 * TradeNotFoundException
 *
 * WHAT:    Signals that no trade exists for the requested {@code tradeRef}.
 * HOW:     Unchecked {@link ReconException} leaf; mapped to HTTP 404 by advice.
 * WHY:     Callers need a typed miss rather than a generic runtime failure so
 *          REST and messaging layers can return Not Found consistently.
 * OBSERVE: Raised from lookup paths when the repository returns empty.
 * ============================================================================
 */
public class TradeNotFoundException extends ReconException {

    /**
     * Creates a not-found exception for a missing trade reference.
     *
     * @param message detail describing which trade reference was missing
     */
    public TradeNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a not-found exception with an underlying cause.
     *
     * @param message detail describing which trade reference was missing
     * @param cause   underlying failure that triggered the lookup miss; may be {@code null}
     */
    public TradeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
