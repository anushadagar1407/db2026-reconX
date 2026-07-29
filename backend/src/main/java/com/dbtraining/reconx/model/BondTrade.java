package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * BondTrade with Builder pattern
 *
 * WHAT:    Fixed-income trade — couponRate, maturityDate, faceValue, isin.
 * HOW:     Same builder pattern as other leaves. {@link #notional()} = faceValue
 *          in the bond's currency.
 * WHY:     Bonds need couponRate/maturity for downstream cashflow modelling.
 *          Modelling them on the trade is the simplest path for the demo.
 * OBSERVE: {@code build()} rejects blank/short ISINs and maturity on or before trade date.
 * ============================================================================
 */
public final class BondTrade implements TradeType {

    private final TradeRef tradeRef;
    private final String isin;
    private final BigDecimal faceValue;
    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private BondTrade(Builder b) {
        this.tradeRef       = b.tradeRef;
        this.isin           = b.isin;
        this.faceValue      = b.faceValue;
        this.couponRate     = b.couponRate;
        this.maturityDate   = b.maturityDate;
        this.currency       = b.currency;
        this.side           = b.side;
        this.tradeDate      = b.tradeDate;
        this.counterpartyId = b.counterpartyId;
    }

    /**
     * Starts a fluent builder for a new bond trade.
     *
     * @return a fresh {@link Builder} with no fields set
     */
    public static Builder builder() { return new Builder(); }

    /** {@inheritDoc} */
    @Override public TradeRef tradeRef()     { return tradeRef; }
    /** {@inheritDoc} */
    @Override public LocalDate tradeDate()   { return tradeDate; }
    /** {@inheritDoc} */
    @Override public AssetClass assetClass() { return AssetClass.BOND; }

    /**
     * Notional equals face value in the bond currency.
     *
     * @return non-null {@link Money} wrapping {@link #faceValue()} and {@link #currency()}
     */
    @Override public Money notional()        { return new Money(faceValue, currency); }

    /**
     * International Securities Identification Number (12 characters).
     *
     * @return non-blank 12-character ISIN
     */
    public String isin()              { return isin; }

    /**
     * Principal / face amount of the bond position.
     *
     * @return strictly positive face value
     */
    public BigDecimal faceValue()     { return faceValue; }

    /**
     * Annual coupon rate as a decimal fraction (e.g. {@code 0.05} = 5%).
     *
     * @return non-negative coupon rate
     */
    public BigDecimal couponRate()    { return couponRate; }

    /**
     * Bond maturity date; must be strictly after {@link #tradeDate()}.
     *
     * @return non-null maturity calendar date
     */
    public LocalDate maturityDate()   { return maturityDate; }

    /**
     * ISO currency of face value and notional.
     *
     * @return non-null trade currency
     */
    public Currency currency()        { return currency; }

    /**
     * Buy or sell direction of this trade.
     *
     * @return non-null {@link Side}
     */
    public Side side()                { return side; }

    /**
     * Internal counterparty identifier (opaque numeric FK).
     *
     * @return counterparty id as stored on the trade
     */
    public long counterpartyId()      { return counterpartyId; }

    /**
     * Two bond trades are equal iff their {@link TradeRef} values are equal.
     *
     * @param o object to compare
     * @return {@code true} when {@code o} is a {@code BondTrade} with the same ref
     */
    @Override public boolean equals(Object o) {
        return (o instanceof BondTrade other) && tradeRef.equals(other.tradeRef());

    }

    /**
     * Hash code derived solely from {@link #tradeRef()}.
     *
     * @return hash consistent with {@link #equals(Object)}
     */
    @Override public int hashCode() { return tradeRef.hashCode(); }

    /**
     * PII-safe summary: ref, isin, face, coupon, maturity, side.
     *
     * @return single-line diagnostic string without counterparty identity details
     */
    @Override public String toString() {
        return "DerivativeTrade[ref=%s, isin=%s, face=%s %s, coupon=%s, maturity=%s, side=%s]"
        .formatted(tradeRef, isin,faceValue.toPlainString(), currency.getCurrencyCode(), couponRate, maturityDate, side);
    }

    /**
     * Fluent builder for {@link BondTrade}. Required fields are validated in {@link #build()}.
     *
     * <p>Setter methods are intentionally undocumented — names match the target fields.
     */
    public static final class Builder {
        private TradeRef tradeRef;
        private String isin;
        private BigDecimal faceValue, couponRate;
        private LocalDate maturityDate, tradeDate;
        private Currency currency;
        private Side side;
        private long counterpartyId;

        /** Creates an empty builder; prefer {@link BondTrade#builder()}. */
        public Builder() {}

        /**
         * Sets the natural key for the trade.
         * @param v validated trade reference required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        /**
         * Sets the 12-character ISIN.
         * @param v ISIN text validated in {@link #build()}
         * @return this builder for chaining
         */
        public Builder isin(String v)              { this.isin = v; return this; }
        /**
         * Sets principal/face amount (must be strictly positive at build).
         * @param v face value required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder faceValue(BigDecimal v)     { this.faceValue = v; return this; }
        /**
         * Sets annual coupon as a decimal fraction (must be non-negative at build).
         * @param v coupon rate required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder couponRate(BigDecimal v)    { this.couponRate = v; return this; }
        /**
         * Sets maturity date (must be strictly after trade date at build).
         * @param v maturity calendar date required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder maturityDate(LocalDate v)   { this.maturityDate = v; return this; }
        /**
         * Sets face-value currency from an ISO code.
         * @param code ISO 4217 currency code for face value
         * @return this builder for chaining
         * @throws IllegalArgumentException if {@code code} is not a known currency
         */
        public Builder currency(String code)       { this.currency = Currency.getInstance(code); return this; }
        /**
         * Sets buy/sell direction.
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
         * Build the immutable {@link BondTrade}, validating required fields and bond invariants.
         *
         * @return a fully-constructed, validated {@code BondTrade} — never {@code null}
         * @throws NullPointerException  if any required field
         *                               ({@code tradeRef}, {@code isin}, {@code faceValue},
         *                               {@code couponRate}, {@code maturityDate}, {@code currency},
         *                               {@code side}, {@code tradeDate}) was not set
         * @throws IllegalStateException if {@code isin} is blank or not 12 characters,
         *                               {@code faceValue} is not strictly positive,
         *                               {@code couponRate} is negative, or
         *                               {@code maturityDate} is not strictly after {@code tradeDate}
         */
        public BondTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(isin, "isin");
            Objects.requireNonNull(faceValue, "faceValue");
            Objects.requireNonNull(couponRate, "couponRate");
            Objects.requireNonNull(maturityDate, "maturityDate");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");

            if (isin.isBlank()) {
                throw new IllegalStateException("isin must not be blank");
            }
            if (isin.length() != 12) {
                throw new IllegalStateException(
                        "isin must contain exactly 12 characters");
            }
            if (faceValue.signum() <= 0) {
                throw new IllegalStateException("faceValue must be > 0");
            }
            if (couponRate.signum() < 0) {
                throw new IllegalStateException("couponRate must be >= 0");
            }
            if (!maturityDate.isAfter(tradeDate)) {
                throw new IllegalStateException(
                        "maturityDate must be after tradeDate");
            }

            return new BondTrade(this);
        }
    }
}
