package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * ============================================================================
 * TradeFactory: build a TradeType by asset-class string
 *
 * WHAT:    Single entry point that takes an asset-class string + a map of
 *          field values and returns the right {@link TradeType} impl.
 * HOW:     Switch on the asset-class string, dispatch to the correct
 *          builder. Map values are cast/parsed per asset class.
 * WHY:     The Kafka consumer + REST POST endpoint both need to convert an
 *          untyped payload into a typed {@code TradeType}. Centralising the
 *          construction here means the parsing logic lives in one place.
 * OBSERVE: TradeFactoryTest.create_unknownAssetClass_throws fails when a
 *          new {@code TradeType} impl is added without updating the switch.
 * HINT:    Sealed hierarchy guarantees that every concrete {@code TradeType} MUST be
 *          listed in {@code TradeType.permits} — so this switch can be made
 *          exhaustive over assetClass enum.
 * ============================================================================
 *
 * <p>This class is not instantiable; use {@link #create(String, Map)}.
 */
public final class TradeFactory {

    private TradeFactory() { }

    /**
     * Builds a typed trade from an asset-class name and a field map.
     *
     * <p>Expected map keys depend on the asset class (see private helpers):
     * equity needs {@code tradeRef}, {@code symbol}, {@code quantity}, {@code price},
     * {@code currency}, {@code side}, {@code tradeDate}, {@code counterpartyId}; FX,
     * bond, and derivative each have their own required key sets.
     *
     * @param assetClass case-insensitive name matching {@link TradeType.AssetClass}
     *                   ({@code EQUITY}, {@code FX}, {@code BOND}, {@code DERIVATIVE})
     * @param p          field bag; must contain the keys required by the chosen asset class
     * @return a fully built, validated {@link TradeType}; never {@code null}
     * @throws NullPointerException     if {@code assetClass} or {@code p} is {@code null},
     *                                  or a required map value is missing/{@code null}
     * @throws IllegalArgumentException if {@code assetClass} is not a known
     *                                  {@link TradeType.AssetClass} name, a nested value
     *                                  fails parsing (e.g. bad {@link TradeRef}), or a
     *                                  builder invariant fails during construction
     * @throws ClassCastException       if a map value has an unexpected runtime type
     * @throws java.time.format.DateTimeParseException if a date field is not ISO-8601
     */
    public static TradeType create(String assetClass, Map<String, Object> p) {
        TradeType.AssetClass ac = TradeType.AssetClass.valueOf(assetClass.toUpperCase());
        return switch (ac) {
            case EQUITY     -> equity(p);
            case FX         -> fx(p);
            case BOND       -> bond(p);
            case DERIVATIVE -> derivative(p);
        };
    }

    /**
     * TICKET-ADV023:
     *   Build an EquityTrade from the map. Expected keys: tradeRef, symbol,
     *   quantity, price, currency, side, tradeDate, counterpartyId.
     */
    private static EquityTrade equity(Map<String, Object> p) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of((String) p.get("tradeRef")))
                .instrumentSymbol((String) p.get("symbol"))
                .quantity(new BigDecimal(p.get("quantity").toString()))
                .price(new BigDecimal(p.get("price").toString()))
                .currency((String) p.get("currency"))
                .side(Side.valueOf((String) p.get("side")))
                .tradeDate(LocalDate.parse((String) p.get("tradeDate")))
                .counterpartyId(((Number) p.get("counterpartyId")).longValue())
                .build();
    }


    /**
     * TICKET-ADV023:
     *   Build an FXTrade from the map. Expected keys: tradeRef, ccy1, ccy2,
     *   notionalCcy1, fxRate, side, tradeDate, counterpartyId.
     */
    private static FXTrade fx(Map<String, Object> p) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of((String) p.get("tradeRef")))
                .ccy1((String) p.get("ccy1"))
                .ccy2((String) p.get("ccy2"))
                .notionalCcy1(new BigDecimal(p.get("notionalCcy1").toString()))
                .fxRate(new BigDecimal(p.get("fxRate").toString()))
                .side(Side.valueOf((String) p.get("side")))
                .tradeDate(LocalDate.parse((String) p.get("tradeDate")))
                .counterpartyId(((Number) p.get("counterpartyId")).longValue())
                .build();
    }

    /**
     * TICKET-ADV023:
     *   Build a BondTrade from the map. Expected keys: tradeRef, isin,
     *   faceValue, couponRate, maturityDate, currency, side, tradeDate,
     *   counterpartyId.
     */
    private static BondTrade bond(Map<String, Object> p) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of((String) p.get("tradeRef")))
                .isin((String) p.get("isin"))
                .faceValue(new BigDecimal(p.get("faceValue").toString()))
                .couponRate(new BigDecimal(p.get("couponRate").toString()))
                .maturityDate(LocalDate.parse((String) p.get("maturityDate")))
                .currency((String) p.get("currency"))
                .side(Side.valueOf((String) p.get("side")))
                .tradeDate(LocalDate.parse((String) p.get("tradeDate")))
                .counterpartyId(((Number) p.get("counterpartyId")).longValue())
                .build();
    }

    /**
     * TICKET-ADV023:
     *   Build a DerivativeTrade from the map. Expected keys: tradeRef,
     *   underlying, strike, quantity, expiry, optionType, currency, side,
     *   tradeDate, counterpartyId.
     */
    private static DerivativeTrade derivative(Map<String, Object> p) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of((String) p.get("tradeRef")))
                .underlying((String) p.get("underlying"))
                .strike(new BigDecimal(p.get("strike").toString()))
                .quantity(new BigDecimal(p.get("quantity").toString()))
                .expiry(LocalDate.parse((String) p.get("expiry")))
                .optionType(DerivativeTrade.OptionType.valueOf((String) p.get("optionType")))
                .currency((String) p.get("currency"))
                .side(Side.valueOf((String) p.get("side")))
                .tradeDate(LocalDate.parse((String) p.get("tradeDate")))
                .counterpartyId(((Number) p.get("counterpartyId")).longValue())
                .build();
    }
}
