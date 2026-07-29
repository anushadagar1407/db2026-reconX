package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * ============================================================================
 * Sealed interface TradeType
 *
 * WHAT:    Sealed root of the trade hierarchy. Only the four named
 *          permitted classes can implement it. Any new asset class needs an
 *          explicit code change here — by design.
 * HOW:     {@code sealed ... permits ...} on Java 25.
 * WHY:     Without sealing, anyone could write their own {@code Trade} subclass and
 *          slip through the reconciliation engine's pattern-matching switch.
 *          Sealing turns the engine's switch into an exhaustive one — the
 *          compiler enforces that every case is handled.
 * OBSERVE: Removing {@code permits BondTrade} causes a compile error in
 *          ReconciliationEngine's switch expression.
 * HINT:    See Day 2 trainer guide §"Workshop 2A — sealed hierarchy" for the
 *          design discussion.
 * ============================================================================
 *
 * <p>Comparable natural ordering (most-recent trade first).
 * {@code equals}/{@code hashCode} are based on {@link TradeRef} (the natural key).
 *
 * <p>The shared {@link #NATURAL} comparator lives on this interface so every
 * implementation uses the same ordering rule — there is no per-class
 * {@code compareTo} override to forget when adding a new field.
 */
public sealed interface TradeType
        extends Comparable<TradeType>
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    /**
     * Stable natural key for the trade.
     *
     * @return non-null trade reference that drives equality and hashing
     */
    TradeRef tradeRef();

    /**
     * Notional value used in reconciliation roll-ups and summaries.
     *
     * @return non-null monetary notional in the trade's reporting currency
     */
    Money notional();

    /**
     * Business date on which the trade was struck.
     *
     * @return non-null trade date (not settlement date)
     */
    LocalDate tradeDate();

    /**
     * Asset-class discriminator for switch expressions and persistence mapping.
     *
     * @return non-null {@link AssetClass} identifying the concrete trade kind
     */
    AssetClass assetClass();

    /**
     * Shared natural order: newest {@link #tradeDate()} first, then
     * {@link TradeRef#value()} ascending as a stable tie-breaker.
     */
    Comparator<TradeType> NATURAL = Comparator
            .comparing(TradeType::tradeDate).reversed()
            .thenComparing(t -> t.tradeRef().value());

    /**
     * Compares this trade to another using {@link #NATURAL}.
     *
     * @param other trade to compare against; must not be {@code null}
     * @return negative if this ranks before {@code other}, zero if equal under
     *         natural order, positive if this ranks after
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @Override
    default int compareTo(TradeType other) {
        return NATURAL.compare(this, other);
    }

    /**
     * ============================================================================
     * WHAT:    Closed set of asset classes that map 1:1 to sealed {@link TradeType} leaves.
     * HOW:     Simple enum; factory and persistence switches dispatch on these values.
     * WHY:     A free-form string would allow unknown classes at runtime; the enum
     *          keeps the set aligned with {@code permits} on {@link TradeType}.
     * ============================================================================
     */
    enum AssetClass {
        /** Cash equity / share trades. */
        EQUITY,
        /** Foreign-exchange spot or forward trades. */
        FX,
        /** Fixed-income bond trades. */
        BOND,
        /** Option and other derivative trades. */
        DERIVATIVE
    }
}
