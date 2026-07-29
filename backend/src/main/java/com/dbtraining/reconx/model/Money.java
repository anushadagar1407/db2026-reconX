package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * Immutable value object: Money
 *
 * WHAT:    Record bundling a {@link BigDecimal} amount with a {@link Currency}.
 *          Used everywhere a monetary value crosses a boundary (DTO, event,
 *          metric).
 * HOW:     Compact constructor enforces: non-null amount, non-null currency,
 *          non-negative amount. {@link BigDecimal} (not double) prevents
 *          accumulating floating-point error on aggregations.
 * WHY:     Passing raw {@code BigDecimal} around loses currency context — a USD 100
 *          can be silently added to a EUR 100. Money makes the mismatch
 *          fail at the type level: {@link #plus(Money)} throws if currencies differ.
 * OBSERVE: {@code Money.of("100.00","USD").plus(Money.of("50","EUR"))} throws.
 *          {@code Money.of("100","USD").plus(Money.of("50","USD"))} returns 150 USD.
 * ============================================================================
 *
 * @param amount   non-negative monetary magnitude
 * @param currency ISO currency of {@code amount}
 */
public record Money(BigDecimal amount, Currency currency) {

    /**
     * Compact constructor enforcing non-null components and a non-negative amount.
     *
     * @throws NullPointerException     if {@code amount} or {@code currency} is {@code null}
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }
    }

    /**
     * Parses a decimal amount string and an ISO currency code into {@code Money}.
     *
     * @param amount       decimal text accepted by {@link BigDecimal#BigDecimal(String)}
     * @param currencyCode ISO 4217 currency code (e.g. {@code "USD"})
     * @return a non-null {@code Money} with a non-negative amount
     * @throws NullPointerException     if either argument is {@code null}
     * @throws NumberFormatException    if {@code amount} is not a valid decimal
     * @throws IllegalArgumentException if the currency code is unknown or amount is negative
     */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Builds {@code Money} from an existing amount and ISO currency code.
     *
     * @param amount       non-negative magnitude; must not be {@code null}
     * @param currencyCode ISO 4217 currency code (e.g. {@code "EUR"})
     * @return a non-null {@code Money}
     * @throws NullPointerException     if {@code amount} or {@code currencyCode} is {@code null}
     * @throws IllegalArgumentException if the currency code is unknown or amount is negative
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Adds another {@code Money} of the same currency.
     *
     * @param other addend; must share this instance's currency
     * @return a new {@code Money} whose amount is the sum; never {@code null}
     * @throws NullPointerException     if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other}'s currency differs from this one
     */
    public Money plus(Money other) {
        if (this.currency.equals(other.currency)) {
            BigDecimal newAmount = this.amount.add(other.amount);
            return new Money(newAmount, this.currency);
        } else {
            throw new IllegalArgumentException("Mismatched Currencies");
        }
    }

    /**
     * Multiplies the amount by a scalar, keeping the same currency.
     *
     * @param multiplier scale factor applied to {@link #amount()}; must not be {@code null}
     * @return a new {@code Money} with {@code amount * multiplier}
     * @throws NullPointerException     if {@code multiplier} is {@code null}
     * @throws IllegalArgumentException if the product is negative
     */
    public Money times(BigDecimal multiplier) {
        return Money.of(this.amount.multiply(multiplier), this.currency.getCurrencyCode());
    }
}
