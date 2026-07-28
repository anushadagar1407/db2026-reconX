package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV020 — FXTrade with Builder pattern
 *
 * WHAT:    FX spot/forward trade — two currencies, a notional in ccy1, and
 *          an fxRate.
 * HOW:     Same builder pattern as EquityTrade. notional() converts to ccy2
 *          via fxRate so reconciliation rolls up in the trade's quote ccy.
 * WHY:     FX has two natural sides — a EUR/USD trade is BOTH a buy of EUR
 *          AND a sell of USD. Modelling that with two distinct currency
 *          fields makes settlement-side reasoning explicit.
 * OBSERVE: notional().currency() == ccy2; .amount() == notionalCcy1 * fxRate.
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

    public static Builder builder() { return new Builder(); }

    @Override public TradeRef tradeRef()     { return tradeRef; }
    @Override public LocalDate tradeDate()   { return tradeDate; }
    @Override public AssetClass assetClass() { return AssetClass.FX; }

    /** Notional in ccy2 = notionalCcy1 * fxRate. */
    @Override public Money notional() {
        return new Money(notionalCcy1.multiply(fxRate), ccy2);
    }

    public Currency ccy1()           { return ccy1; }
    public Currency ccy2()           { return ccy2; }
    public BigDecimal notionalCcy1() { return notionalCcy1; }
    public BigDecimal fxRate()       { return fxRate; }
    public Side side()               { return side; }
    public long counterpartyId()     { return counterpartyId; }

    @Override public boolean equals(Object o) {
        return (o instanceof FXTrade other) && tradeRef.equals(other.tradeRef());
    }
    @Override public int hashCode() { return tradeRef.hashCode(); }


    @Override public String toString() {
        // TODO(TICKET-ADV030): "FXTrade[ref=..., CCY1/CCY2, notional=... CCY1, rate=..., side=...]"
        throw new UnsupportedOperationException("TICKET-ADV030");
    }

    public static final class Builder {
        private TradeRef tradeRef;
        private Currency ccy1, ccy2;
        private BigDecimal notionalCcy1, fxRate;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef v)        { this.tradeRef = v; return this; }
        public Builder ccy1(String code)           { this.ccy1 = Currency.getInstance(code); return this; }
        public Builder ccy2(String code)           { this.ccy2 = Currency.getInstance(code); return this; }
        public Builder notionalCcy1(BigDecimal v)  { this.notionalCcy1 = v; return this; }
        public Builder fxRate(BigDecimal v)        { this.fxRate = v; return this; }
        public Builder side(Side v)                { this.side = v; return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        public Builder counterpartyId(long v)      { this.counterpartyId = v; return this; }

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
