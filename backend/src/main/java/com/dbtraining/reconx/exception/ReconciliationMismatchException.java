package com.dbtraining.reconx.exception;

/**
 * ============================================================================
 * ReconciliationMismatchException
 *
 * WHAT:    Signals that an internal vs external trade pair failed the active match rule.
 * HOW:     Unchecked {@link ReconException} leaf; mapped to HTTP 422 Unprocessable by advice.
 * WHY:     A mismatch is not a client syntax error (400) nor a missing resource (404) —
 *          the payload is understood but cannot be reconciled.
 * OBSERVE: Raised when {@link com.dbtraining.reconx.model.ReconciliationRule#matches}
 *          returns {@code false} for a required match path.
 * ============================================================================
 */
public class ReconciliationMismatchException extends ReconException {

    /**
     * Creates a mismatch exception for an unreconciled trade pair.
     *
     * @param message detail describing the internal/external divergence
     */
    public ReconciliationMismatchException(String message) {
        super(message);
    }

    /**
     * Creates a mismatch exception with an underlying cause.
     *
     * @param message detail describing the internal/external divergence
     * @param cause   underlying comparison failure; may be {@code null}
     */
    public ReconciliationMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
