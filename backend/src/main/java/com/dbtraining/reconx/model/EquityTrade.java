package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * EquityTrade with Builder pattern
 *
 * WHAT:    Concrete {@link TradeType} for equity (cash share) trades.
 * HOW:     Final class, all fields final, no setters. Construction is via the
 *          nested {@link Builder} which validates in {@link Builder#build()}.
 * WHY:     Eight required fields on a single constructor is unreadable at
 *          the call site. Builder gives named arguments, makes the validity
 *          check a single chokepoint, and the object stays immutable.
 * OBSERVE: Calling {@code build()} with a missing required field throws
 *          {@link NullPointerException}; invalid quantity/price throw
 *          {@link IllegalStateException} — verified by EquityTradeTest.
 * HINT:    Same shape applied to {@link FXTrade}/{@link BondTrade}/{@link DerivativeTrade}.
 * ============================================================================
 *
 * <p>Equality and hashing are keyed solely on {@link TradeRef}.
 * {@link #toString()} omits PII and prints reference/symbol/qty/price/side.
 */
public final class EquityTrade implements TradeType {

    private final TradeRef tradeRef;
    private final String instrumentSymbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private EquityTrade(Builder b) {
        this.tradeRef         = b.tradeRef;
        this.instrumentSymbol = b.instrumentSymbol;
        this.quantity         = b.quantity;
        this.price            = b.price;
        this.currency         = b.currency;
        this.side             = b.side;
        this.tradeDate        = b.tradeDate;
        this.counterpartyId   = b.counterpartyId;
    }

    /**
     * Starts a fluent builder for a new equity trade.
     *
     * @return a fresh {@link Builder} with no fields set
     */
    public static Builder builder() { return new Builder(); }

    /** {@inheritDoc} */
    @Override public TradeRef tradeRef()    { return tradeRef; }
    /** {@inheritDoc} */
    @Override public LocalDate tradeDate()  { return tradeDate; }
    /** {@inheritDoc} */
    @Override public AssetClass assetClass(){ return AssetClass.EQUITY; }

    /**
     * Notional = {@code quantity * price} in the trade currency.
     *
     * @return non-null {@link Money} representing cash notional
     */
    @Override public Money notional() {
        // TODO(TICKET-ADV019): return new Money(quantity * price, currency).
        return new Money(quantity.multiply(price), currency);
    }

    /**
     * Exchange ticker / instrument symbol for the equity.
     *
     * @return non-blank symbol string set at build time
     */
    public String instrumentSymbol() { return instrumentSymbol; }

    /**
     * Number of shares traded.
     *
     * @return strictly positive quantity
     */
    public BigDecimal quantity()     { return quantity; }

    /**
     * Unit price per share in {@link #currency()}.
     *
     * @return strictly positive price
     */
    public BigDecimal price()        { return price; }

    /**
     * ISO currency of price and notional.
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
     * Two equity trades are equal iff their {@link TradeRef} values are equal.
     *
     * @param o object to compare
     * @return {@code true} when {@code o} is an {@code EquityTrade} with the same ref
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof EquityTrade other) && tradeRef.equals(other.tradeRef);
    }

    /**
     * Hash code derived solely from {@link #tradeRef()}.
     *
     * @return hash consistent with {@link #equals(Object)}
     */
    @Override public int hashCode() { return tradeRef.hashCode(); }

    /**
     * PII-safe summary: ref, symbol, quantity, price, currency code, side.
     *
     * @return single-line diagnostic string without counterparty identity details
     */
    @Override
    public String toString() {
        return "EquityTrade[ref=%s, symbol=%s, qty=%s, price=%s %s, side=%s]"
        .formatted(tradeRef, instrumentSymbol, quantity, price, currency.getCurrencyCode(), side);
    }

    /**
     * Fluent builder for {@link EquityTrade}. Required fields are validated in {@link #build()}.
     *
     * <p>Setter methods are intentionally undocumented — names match the target fields.
     */
    public static final class Builder {
        private TradeRef tradeRef;
        private String instrumentSymbol;
        private BigDecimal quantity;
        private BigDecimal price;
        private Currency currency;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        /** Creates an empty builder; prefer {@link EquityTrade#builder()}. */
        public Builder() {}

        /**
         * Sets the natural key for the trade.
         * @param v validated trade reference required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeRef(TradeRef v)           { this.tradeRef = v;        return this; }
        /**
         * Sets the exchange ticker / instrument symbol.
         * @param v symbol text required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder instrumentSymbol(String v)     { this.instrumentSymbol = v; return this; }
        /**
         * Sets the share quantity (must be strictly positive at build).
         * @param v quantity required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder quantity(BigDecimal v)         { this.quantity = v;        return this; }
        /**
         * Sets the unit price (must be strictly positive at build).
         * @param v price required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder price(BigDecimal v)            { this.price = v;           return this; }
        /**
         * Sets the trade currency object.
         * @param v ISO currency required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder currency(Currency v)           { this.currency = v;        return this; }
        /**
         * Resolves an ISO 4217 code to a {@link Currency}.
         *
         * @param code ISO currency code such as {@code "USD"}
         * @return this builder for chaining
         * @throws NullPointerException     if {@code code} is {@code null}
         * @throws IllegalArgumentException if {@code code} is not a known currency
         */
        public Builder currency(String code)          { return currency(Currency.getInstance(code)); }
        /**
         * Sets buy/sell direction.
         * @param v side required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder side(Side v)                   { this.side = v;            return this; }
        /**
         * Sets the business trade date.
         * @param v calendar date required by {@link #build()}
         * @return this builder for chaining
         */
        public Builder tradeDate(LocalDate v)         { this.tradeDate = v;       return this; }
        /**
         * Sets the opaque counterparty foreign key.
         * @param v counterparty identifier stored on the trade
         * @return this builder for chaining
         */
        public Builder counterpartyId(long v)         { this.counterpartyId = v;  return this; }

        /**
         * Build the immutable {@link EquityTrade}, validating that every required
         * field is set and that all invariants hold.
         *
         * @return a fully-constructed, validated {@code EquityTrade} — never {@code null}
         * @throws NullPointerException  if any required field
         *                               ({@code tradeRef}, {@code instrumentSymbol},
         *                               {@code quantity}, {@code price}, {@code currency},
         *                               {@code side}, {@code tradeDate})
         *                               was not set
         * @throws IllegalStateException if {@code quantity} is not strictly positive, or
         *                               {@code price} is not strictly positive
         */
        public EquityTrade build() {
            // TODO(TICKET-ADV019):
            //   - Objects.requireNonNull each required field (tradeRef, instrumentSymbol,
            //     quantity, price, currency, side, tradeDate).
            //   - quantity and price must be > 0 (IllegalStateException otherwise).
            //   - return new EquityTrade(this).
            Objects.requireNonNull(tradeRef, "tradeRef is required");
            Objects.requireNonNull(instrumentSymbol, "instrumentSymbol is required");
            Objects.requireNonNull(quantity, "quantity is required");
            Objects.requireNonNull(price, "price is required");
            Objects.requireNonNull(currency, "currency is required");
            Objects.requireNonNull(side, "side is required");
            Objects.requireNonNull(tradeDate, "tradeDate is required");

            if (quantity.signum() <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }
            if (price.signum() <= 0) {
                throw new IllegalStateException("price must be > 0");
            }

            return new EquityTrade(this);
        }
    }
}
