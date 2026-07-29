package com.dbtraining.reconx.exception;

/**
 * ============================================================================
 * InvalidTradeException
 *
 * WHAT:    Signals that a trade failed business validation after structural parsing.
 * HOW:     Unchecked {@link ReconException} leaf; mapped to HTTP 400 Bad Request by advice.
 * WHY:     Separates "malformed payload" / domain rule breaches from not-found and conflict.
 * OBSERVE: Use when invariants outside builder construction still fail at the service layer.
 * ============================================================================
 */
public class InvalidTradeException extends ReconException {

    /**
     * Creates a validation exception for a business-rule breach.
     *
     * @param message detail describing which business rule was violated
     */
    public InvalidTradeException(String message) {
        super(message);
    }

    /**
     * Creates a validation exception with an underlying cause.
     *
     * @param message detail describing which business rule was violated
     * @param cause   underlying validation failure; may be {@code null}
     */
    public InvalidTradeException(String message, Throwable cause) {
        super(message, cause);
    }
}
