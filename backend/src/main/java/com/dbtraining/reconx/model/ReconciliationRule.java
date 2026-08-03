package com.dbtraining.reconx.model;

import java.math.BigDecimal;

/**
 * ============================================================================
 * ReconciliationRule enum with configurable thresholds
 *
 * WHAT:    Each enum value carries its own price tolerance (%) and quantity
 *          tolerance (absolute units). {@link #matches} returns true if the
 *          internal vs external trade pair is within tolerance.
 * HOW:     Enum-with-state pattern — instance fields + a behaviour method.
 * WHY:     Putting the rule on the enum keeps "what is a match" co-located
 *          with the rule's name, so the reconciliation engine is just:
 *          {@code if (rule.matches(internal, external)) ... matched ...}.
 * OBSERVE: {@code PRICE_TOLERANCE_1PCT.matches(p, p*1.005)} is true; {@code *1.02} is false.
 * ============================================================================
 */
public enum ReconciliationRule {

    /** Match only when price and quantity are identical (zero tolerance). */
    EXACT(BigDecimal.ZERO, BigDecimal.ZERO),
    /** Allow up to 1% relative price difference; quantity must match exactly. */
    PRICE_TOLERANCE_1PCT(new BigDecimal("0.01"), BigDecimal.ZERO),
    /** Allow up to 50 basis points (0.5%) relative price difference. */
    PRICE_TOLERANCE_50BPS(new BigDecimal("0.005"), BigDecimal.ZERO),
    /** Exact price; allow up to 5 absolute quantity units of drift. */
    QTY_TOLERANCE_5UNITS(BigDecimal.ZERO, new BigDecimal("5")),
    /** Loose: 5% price tolerance and 10 absolute quantity units. */
    LOOSE(new BigDecimal("0.05"), new BigDecimal("10"));

    private final BigDecimal priceTolerancePct;
    private final BigDecimal qtyToleranceAbs;

    ReconciliationRule(BigDecimal priceTolerancePct, BigDecimal qtyToleranceAbs) {
        this.priceTolerancePct = priceTolerancePct;
        this.qtyToleranceAbs   = qtyToleranceAbs;
    }

    /**
     * Relative price tolerance as a fraction of internal price (e.g. {@code 0.01} = 1%).
     *
     * @return non-null non-negative tolerance fraction
     */
    public BigDecimal priceTolerancePct() { return priceTolerancePct; }

    /**
     * Absolute quantity tolerance in the same units as the trade quantity.
     *
     * @return non-null non-negative absolute quantity allowance
     */
    public BigDecimal qtyToleranceAbs()   { return qtyToleranceAbs; }

    /**
     * Decide whether two prices/quantities are within this rule's tolerance.
     *
     * <p>Price difference is measured as
     * {@code |internalPrice - externalPrice| / internalPrice} when internal price
     * is positive; a zero internal price with a non-zero external price is treated
     * as out of tolerance.
     *
     * @param internalPrice price on the internal book; must not be {@code null}
     * @param internalQty   quantity on the internal book; must not be {@code null}
     * @param externalPrice price from the external source; must not be {@code null}
     * @param externalQty   quantity from the external source; must not be {@code null}
     * @return {@code true} if both the relative price gap and absolute quantity gap
     *         are within this rule's tolerances
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException  if division scale requirements cannot be satisfied
     */
    public boolean matches(BigDecimal internalPrice, BigDecimal internalQty,
                           BigDecimal externalPrice, BigDecimal externalQty) {
        return matches(internalPrice, internalQty, externalPrice, externalQty, priceTolerancePct);
    }

    public boolean matches(BigDecimal internalPrice,
                           BigDecimal internalQty,
                           BigDecimal externalPrice,
                           BigDecimal externalQty,
                           BigDecimal effectivePriceTolerance) {
        if (effectivePriceTolerance.signum() < 0) {
            throw new IllegalArgumentException("price tolerance must not be negative");
        }

        BigDecimal priceDiff = internalPrice.add(externalPrice.negate()).abs();
        BigDecimal priceDiffPct;
        if (internalPrice.compareTo(BigDecimal.ZERO) > 0) {
            priceDiffPct = priceDiff.divide(internalPrice);
        } else if (priceDiff.compareTo(BigDecimal.ZERO) == 0) {
            priceDiffPct = BigDecimal.ZERO;
        } else {
            priceDiffPct = new BigDecimal("100");
        }

        BigDecimal qtyDiff = internalQty.add(externalQty.negate()).abs();
        return priceDiffPct.compareTo(effectivePriceTolerance) <= 0
                && qtyDiff.compareTo(qtyToleranceAbs) <= 0;
    }
}
