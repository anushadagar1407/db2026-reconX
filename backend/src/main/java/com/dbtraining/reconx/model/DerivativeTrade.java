package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * DerivativeTrade with Builder pattern
 *
 * WHAT:    Option/derivative trade — underlying, strike, expiry, optionType.
 * HOW:     Same builder pattern as other leaves. {@link #notional()} =
 *          {@code strike * quantity} in the trade's currency (simplified —
 *          real derivatives use delta-adjusted notionals).
 * WHY:     Options need strike/expiry/type for risk and matching; keeping them
 *          on the trade keeps the sealed hierarchy self-describing.
 * OBSERVE: {@code build()} rejects blank underlying, non-positive strike/qty,
 *          and expiry on or before trade date (historical expired options remain valid).
 * ============================================================================
 */
public final class DerivativeTrade implements TradeType {

    /**
     * ============================================================================
     * WHAT:    Call vs put option style for this derivative.
     * HOW:     Two-value enum used by builder and factory map parsing.
     * WHY:     Prevents free-text typos and enables exhaustive switches.
     * ============================================================================
     */
    public enum OptionType {
        /** Right to buy the underlying at strike. */
        CALL,
        /** Right to sell the underlying at strike. */
        PUT
    }

    private final TradeRef tradeRef;
    private final String underlying;
    private final BigDecimal strike;
    private final BigDecimal quantity;
    private final LocalDate expiry;
    private final OptionType optionType;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private DerivativeTrade(Builder b) {
        this.tradeRef       = b.tradeRef;
        this.underlying     = b.underlying;
        this.strike         = b.strike;
        this.quantity       = b.quantity;
        this.expiry         = b.expiry;
        this.optionType     = b.optionType;
        this.currency       = b.currency;
        this.side           = b.side;
        this.tradeDate      = b.tradeDate;
        this.counterpartyId = b.counterpartyId;
    }

    /**
     * Starts a fluent builder for a new derivative trade.
     *
     * @return a fresh {@link Builder} with no fields set
     */
    public static Builder builder() { return new Builder(); }

    /** {@inheritDoc} */
    @Override public TradeRef tradeRef()     { return tradeRef; }
    /** {@inheritDoc} */
    @Override public LocalDate tradeDate()   { return tradeDate; }
    /** {@inheritDoc} */
    @Override public AssetClass assetClass() { return AssetClass.DERIVATIVE; }

    /**
     * Simplified notional = {@code strike * quantity} in the trade currency.
     *
     * @return non-null {@link Money} using strike×quantity as the amount
     */
    @Override public Money notional() {
        return new Money(strike.multiply(quantity), currency);
    }

    /**
     * Underlying instrument symbol the option is written on.
     *
     * @return non-blank underlying identifier
     */
    public String underlying()       { return underlying; }

    /**
     * Option strike price in {@link #currency()}.
     *
     * @return strictly positive strike
     */
    public BigDecimal strike()       { return strike; }

    /**
     * Number of contracts / units.
     *
     * @return strictly positive quantity
     */
    public BigDecimal quantity()     { return quantity; }

    /**
     * Option expiry date; must be strictly after {@link #tradeDate()}.
     *
     * @return non-null expiry calendar date
     */
    public LocalDate expiry()        { return expiry; }

    /**
     * Whether this is a call or a put.
     *
     * @return non-null {@link OptionType}
     */
    public OptionType optionType()   { return optionType; }

    /**
     * ISO currency of strike and notional.
     *
     * @return non-null trade currency
     */
    public Currency currency()       { return currency; }

    /**
     * Buy or sell direction of this trade.
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
     * Two derivative trades are equal iff their {@link TradeRef} values are equal.
     *
     * @param o object to compare
     * @return {@code true} when {@code o} is a {@code DerivativeTrade} with the same ref
     */
    @Override public boolean equals(Object o) {
        return (o instanceof DerivativeTrade other) && tradeRef.equals(other.tradeRef());

    }

    /**
     * Hash code derived solely from {@link #tradeRef()}.
     *
     * @return hash consistent with {@link #equals(Object)}
     */
    @Override public int hashCode() { return tradeRef.hashCode(); }


    /**
     * PII-safe summary: ref, strike, currency, quantity, expiry, side.
     *
     * @return single-line diagnostic string without counterparty identity details
     */
    @Override public String toString() {
        // TODO(TICKET-ADV030): "DerivativeTrade[ref=..., TYPE UNDERLYING on date, strike=... CCY, qty=..., expiry=..., side=...]"
        return "DerivativeTrade[ref=%s, TYPE UNDERLYING on date, strike=%s %s, qty=%s, expiry=%s, side=%s]"
        .formatted(tradeRef, strike, currency.getCurrencyCode(), quantity, expiry.toString(), side);
    }

    /**
     * Fluent builder for {@link DerivativeTrade}. Required fields are validated in {@link #build()}.
     *
     * <p>Setter methods are intentionally undocumented — names match the target fields.
     */
    public static final class Builder {
        private TradeRef tradeRef;
        private String underlying;
        private BigDecimal strike, quantity;
        private LocalDate expiry, tradeDate;
        private OptionType optionType;
        private Currency currency;
        private Side side;
        private long counterpartyId;

        /** Creates an empty builder; prefer {@link DerivativeTrade#builder()}. */
        public Builder() {}

        /**
         * Sets the natural key for the trade.
         * @param v validated trade reference required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        /**
         * Sets the underlying instrument identifier (non-blank at build).
         * @param v underlying symbol required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder underlying(String v)        { this.underlying = v; return this; }
        /**
         * Sets option strike (must be strictly positive at build).
         * @param v strike required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder strike(BigDecimal v)        { this.strike = v; return this; }
        /**
         * Sets contract quantity (must be strictly positive at build).
         * @param v quantity required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder quantity(BigDecimal v)      { this.quantity = v; return this; }
        /**
         * Sets expiry date (must be strictly after trade date at build).
         * @param v expiry calendar date required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder expiry(LocalDate v)         { this.expiry = v; return this; }
        /**
         * Sets call vs put style.
         * @param v option type required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder optionType(OptionType v)    { this.optionType = v; return this; }
        /**
         * Sets strike/notional currency from an ISO code.
         * @param code ISO 4217 currency code for strike/notional
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
         * Build the immutable {@link DerivativeTrade}, validating required fields and option invariants.
         *
         * @return a fully-constructed, validated {@code DerivativeTrade} — never {@code null}
         * @throws NullPointerException  if any required field
         *                               ({@code tradeRef}, {@code underlying}, {@code strike},
         *                               {@code quantity}, {@code expiry}, {@code optionType},
         *                               {@code currency}, {@code side}, {@code tradeDate})
         *                               was not set
         * @throws IllegalStateException if {@code underlying} is blank,
         *                               {@code strike} or {@code quantity} is not strictly positive,
         *                               or {@code expiry} is not strictly after {@code tradeDate}
         */
        public DerivativeTrade build() {
            Objects.requireNonNull(tradeRef,   "tradeRef");
            Objects.requireNonNull(underlying, "underlying");
            Objects.requireNonNull(strike,     "strike");
            Objects.requireNonNull(quantity,   "quantity");
            Objects.requireNonNull(expiry,     "expiry");
            Objects.requireNonNull(optionType, "optionType");
            Objects.requireNonNull(currency,   "currency");
            Objects.requireNonNull(side,       "side");
            Objects.requireNonNull(tradeDate,  "tradeDate");

            if (underlying.isBlank()) {
                throw new IllegalStateException("underlying must not be blank");
            }
            if (strike.signum() <= 0) {
                throw new IllegalStateException("strike must be > 0");
            }
            if (quantity.signum() <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }

            /*
             * Compare expiry with the trade date, not LocalDate.now().
             * An already-expired derivative is still valid historical data.
            */
            if (!expiry.isAfter(tradeDate)) {
                throw new IllegalStateException("expiry cannot be before tradeDate");
            }

            return new DerivativeTrade(this);
        }
    }
}
