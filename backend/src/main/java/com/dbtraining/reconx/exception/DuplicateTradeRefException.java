package com.dbtraining.reconx.exception;

/**
 * ============================================================================
 * DuplicateTradeRefException
 *
 * WHAT:    Signals an attempt to create a trade whose {@code tradeRef} already exists.
 * HOW:     Unchecked {@link ReconException} leaf; mapped to HTTP 409 Conflict by advice.
 * WHY:     Trade references are natural keys — silent overwrite would corrupt the book.
 * OBSERVE: Typically raised on unique-constraint violations at the service boundary.
 * ============================================================================
 */
public class DuplicateTradeRefException extends ReconException {

    /**
     * Creates a conflict exception for a duplicate trade reference.
     *
     * @param message detail identifying the conflicting trade reference
     */
    public DuplicateTradeRefException(String message) {
        super(message);
    }

    /**
     * Creates a conflict exception with an underlying cause.
     *
     * @param message detail identifying the conflicting trade reference
     * @param cause   underlying persistence or validation failure; may be {@code null}
     */
    public DuplicateTradeRefException(String message, Throwable cause) {
        super(message, cause);
    }
}
