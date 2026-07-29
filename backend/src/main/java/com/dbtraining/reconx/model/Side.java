package com.dbtraining.reconx.model;

/**
 * ============================================================================
 * Side
 *
 * WHAT:    Trade direction: {@link #BUY} (we acquire) or {@link #SELL} (we dispose).
 *          Used across every {@link TradeType} implementation.
 * HOW:     Tiny two-value enum rather than a free-form String.
 * WHY:     A typo such as {@code "BUYY"} cannot survive compilation; call sites
 *          get exhaustiveness checks in switches.
 * OBSERVE: Passing an unknown string to {@code Side.valueOf} fails fast at the boundary.
 * ============================================================================
 */
public enum Side {
    /** We acquire the instrument / base currency. */
    BUY,
    /** We dispose of the instrument / base currency. */
    SELL
}
