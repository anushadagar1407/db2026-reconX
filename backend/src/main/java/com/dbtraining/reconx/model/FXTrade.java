package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * FXTrade with Builder pattern
 *
 * WHAT:    FX spot/forward trade — two currencies, a notional in ccy1, and
 *          an fxRate.
 * HOW:     Same builder pattern as {@link EquityTrade}. {@link #notional()} converts to ccy2
 *          via fxRate so reconciliation rolls up in the trade's quote ccy.
 * WHY:     FX has two natural sides — a EUR/USD trade is BOTH a buy of EUR
 *          AND a sell of USD. Modelling that with two distinct currency
 *          fields makes settlement-side reasoning explicit.
 * OBSERVE: {@code notional().currency() == ccy2}; {@code .amount() == notionalCcy1 * fxRate}.
 * ============================================================================
 */
public final class FXTrade implements TradeType {

    private final TradeRef tradeRef;
    private final Currency ccy1;
    private final Currency ccy2;
    private final BigDecimal notionalCcy1;
    private final BigDecimal fxRate;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private FXTrade(Builder b) {
        this.tradeRef       = b.tradeRef;
        this.ccy1           = b.ccy1;
        this.ccy2           = b.ccy2;
        this.notionalCcy1   = b.notionalCcy1;
        this.fxRate         = b.fxRate;
        this.side           = b.side;
        this.tradeDate      = b.tradeDate;
        this.counterpartyId = b.counterpartyId;
    }

    /**
     * Starts a fluent builder for a new FX trade.
     *
     * @return a fresh {@link Builder} with no fields set
     */
    public static Builder builder() { return new Builder(); }

    /** {@inheritDoc} */
    @Override public TradeRef tradeRef()     { return tradeRef; }
    /** {@inheritDoc} */
    @Override public LocalDate tradeDate()   { return tradeDate; }
    /** {@inheritDoc} */
    @Override public AssetClass assetClass() { return AssetClass.FX; }

    /**
     * Notional in ccy2 = {@code notionalCcy1 * fxRate}.
     *
     * @return non-null {@link Money} denominated in {@link #ccy2()}
     */
    @Override public Money notional() {
        return new Money(notionalCcy1.multiply(fxRate), ccy2);
    }

    /**
     * Base (dealt) currency of the pair.
     *
     * @return non-null ISO currency for ccy1
     */
    public Currency ccy1()           { return ccy1; }

    /**
     * Quote currency of the pair (also the notional reporting currency).
     *
     * @return non-null ISO currency for ccy2
     */
    public Currency ccy2()           { return ccy2; }

    /**
     * Notional amount expressed in {@link #ccy1()}.
     *
     * @return strictly positive base notional
     */
    public BigDecimal notionalCcy1() { return notionalCcy1; }

    /**
     * FX rate as units of ccy2 per one unit of ccy1.
     *
     * @return strictly positive rate
     */
    public BigDecimal fxRate()       { return fxRate; }

    /**
     * Buy or sell direction from the desk's perspective on ccy1.
     *
     * @return non-null {@link Side}
     */
    public Side side()               { return side; }

    /**
     * Internal counterparty identifier (opaque numeric FK).
     *
     * @return counterparty id as stored on the trade
     */
    public long counterpartyId()     { return counterpartyId; }

    /**
     * Two FX trades are equal iff their {@link TradeRef} values are equal.
     *
     * @param o object to compare
     * @return {@code true} when {@code o} is an {@code FXTrade} with the same ref
     */
    @Override public boolean equals(Object o) {
        return (o instanceof FXTrade other) && tradeRef.equals(other.tradeRef());
    }

    /**
     * Hash code derived solely from {@link #tradeRef()}.
     *
     * @return hash consistent with {@link #equals(Object)}
     */
    @Override public int hashCode() { return tradeRef.hashCode(); }

    /**
     * PII-safe summary: ref, pair, notional, rate, side.
     *
     * @return single-line diagnostic string without counterparty identity details
     */
    @Override public String toString() {
        // TICKET-ADV030: "FXTrade[ref=..., CCY1/CCY2, notional=... CCY1, rate=..., side=...]"
        return "FXTrade[ref=%s, %s/%s,notional=%s %s, rate= %s, side=%s]"
        .formatted(tradeRef, ccy1.getCurrencyCode(), ccy2.getCurrencyCode(), notionalCcy1.toPlainString(), ccy1, fxRate.toPlainString(), side);
    }

    /**
     * Fluent builder for {@link FXTrade}. Required fields are validated in {@link #build()}.
     *
     * <p>Setter methods are intentionally undocumented — names match the target fields.
     */
    public static final class Builder {
        private TradeRef tradeRef;
        private Currency ccy1, ccy2;
        private BigDecimal notionalCcy1, fxRate;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        /** Creates an empty builder; prefer {@link FXTrade#builder()}. */
        public Builder() {}

        /**
         * Sets the natural key for the trade.
         * @param v validated trade reference required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        /**
         * Sets the base (dealt) currency from an ISO code.
         * @param code ISO 4217 code for the base currency
         * @return this builder for chaining
         * @throws IllegalArgumentException if {@code code} is not a known currency
         */
        public Builder ccy1(String code)           { this.ccy1 = Currency.getInstance(code); return this; }
        /**
         * Sets the quote currency from an ISO code.
         * @param code ISO 4217 code for the quote currency
         * @return this builder for chaining
         * @throws IllegalArgumentException if {@code code} is not a known currency
         */
        public Builder ccy2(String code)           { this.ccy2 = Currency.getInstance(code); return this; }
        /**
         * Sets notional amount in ccy1 (must be strictly positive at build).
         * @param v base notional required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder notionalCcy1(BigDecimal v)  { this.notionalCcy1 = v; return this; }
        /**
         * Sets units of ccy2 per one unit of ccy1 (must be strictly positive at build).
         * @param v FX rate required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder fxRate(BigDecimal v)        { this.fxRate = v; return this; }
        /**
         * Sets buy/sell direction on ccy1.
         * @param v side required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder side(Side v)                { this.side = v; return this; }
        /**
         * Sets the business trade date.
         * @param v calendar date required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        /**
         * Sets the opaque counterparty foreign key.
         * @param v counterparty identifier stored on the trade
         * @return this builder for chaining
         */
        public Builder counterpartyId(long v)      { this.counterpartyId = v; return this; }

        /**
         * Build the immutable {@link FXTrade}, validating required fields and pair invariants.
         *
         * @return a fully-constructed, validated {@code FXTrade} — never {@code null}
         * @throws NullPointerException  if any required field
         *                               ({@code tradeRef}, {@code ccy1}, {@code ccy2},
         *                               {@code notionalCcy1}, {@code fxRate}, {@code side},
         *                               {@code tradeDate}) was not set
         * @throws IllegalStateException if {@code ccy1} equals {@code ccy2}, or
         *                               {@code notionalCcy1} / {@code fxRate} is not strictly positive
         */
        public FXTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef is required");
            Objects.requireNonNull(ccy1, "ccy1 is required");
            Objects.requireNonNull(ccy2, "ccy2 is required");
            Objects.requireNonNull(notionalCcy1, "notionalCcy1 is required");
            Objects.requireNonNull(fxRate, "fxRate is required");
            Objects.requireNonNull(side, "side is required");
            Objects.requireNonNull(tradeDate, "tradeDate is required");

            if (ccy1.equals(ccy2)) {
                throw new IllegalStateException("ccy1 and ccy2 must differ");
            }

            if (notionalCcy1.signum() <= 0) {
                throw new IllegalStateException("notionalCcy1 must be > 0");
            }

            if (fxRate.signum() <= 0) {
                throw new IllegalStateException("fxRate must be > 0");
            }

            return new FXTrade(this);
        }
    }
}
