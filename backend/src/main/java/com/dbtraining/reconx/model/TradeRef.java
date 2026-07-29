package com.dbtraining.reconx.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * Immutable value object: TradeRef (natural key for a trade)
 *
 * WHAT:    Strongly-typed wrapper around the trade reference string. Format:
 *          {@code AAA-YYYYMMDD-NNNN} (3 letters, 8-digit date, 4 digits).
 * HOW:     Compact constructor validates against the regex; null and bad
 *          formats fail at construction.
 * WHY:     A bare String "trade reference" can be confused with any other
 *          String — counterparty name, instrument symbol. {@code TradeRef} as a
 *          distinct type makes those mix-ups a compile error.
 * OBSERVE: {@code TradeRef.of("EQU-20260602-0001")} works; {@code .of("foo")} throws.
 * ============================================================================
 *
 * @param value validated reference string in {@code AAA-YYYYMMDD-NNNN} form
 */
public record TradeRef(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}-\\d{8}-\\d{4}$");

    /**
     * Compact constructor: rejects null and strings that do not match the format.
     *
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not {@code AAA-YYYYMMDD-NNNN}
     */
    public TradeRef {
        Objects.requireNonNull(value, "tradeRef value");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tradeRef format '%s' — expected AAA-YYYYMMDD-NNNN".formatted(value));
        }
    }

    /**
     * Factory that validates and wraps a raw reference string.
     *
     * @param value candidate reference; must match {@code AAA-YYYYMMDD-NNNN}
     * @return a validated {@code TradeRef} instance
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} fails the format check
     */
    public static TradeRef of(String value) {
        return new TradeRef(value);
    }

    /**
     * Returns the raw reference string (same as {@link #value()}).
     *
     * @return the validated reference text, never {@code null}
     */
    @Override
    public String toString() {
        return value;
    }
}
