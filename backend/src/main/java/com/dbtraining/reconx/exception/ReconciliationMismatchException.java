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

    private final Long reconBreakId;

    /**
     * Creates a mismatch exception for an unreconciled trade pair.
     *
     * @param message detail describing the internal/external divergence
     */
    public ReconciliationMismatchException(String message) {
        this(message, null, null);
    }

    /**
     * Creates a mismatch exception with an underlying cause.
     *
     * @param message detail describing the internal/external divergence
     * @param cause   underlying comparison failure; may be {@code null}
     */
    public ReconciliationMismatchException(String message, Throwable cause) {
        this(message, null, cause);
    }

    /**
     * Creates a mismatch exception associated with a persisted reconciliation break.
     *
     * @param message      detail describing the internal/external divergence
     * @param reconBreakId identifier of the reconciliation break, when available
     */
    public ReconciliationMismatchException(String message, long reconBreakId) {
        this(message, reconBreakId, null);
    }

    /**
     * Creates a mismatch exception associated with a persisted reconciliation break
     * and an underlying cause.
     *
     * @param message      detail describing the internal/external divergence
     * @param reconBreakId identifier of the reconciliation break, when available
     * @param cause        underlying comparison failure; may be {@code null}
     */
    public ReconciliationMismatchException(String message, long reconBreakId, Throwable cause) {
        this(message, Long.valueOf(reconBreakId), cause);
    }

    private ReconciliationMismatchException(String message, Long reconBreakId, Throwable cause) {
        super(message, cause);
        this.reconBreakId = reconBreakId;
    }

    /**
     * @return the associated reconciliation break identifier, or {@code null} when
     *         the mismatch was raised before a break was persisted
     */
    public Long getReconBreakId() {
        return reconBreakId;
    }
}
