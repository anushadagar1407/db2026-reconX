package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;
/**
 * ============================================================================
 * TICKET-ADV034 — Trade analytics with Collectors (groupingBy + summarizing)
 * TICKET-ADV035 — VWAP calculator using Streams + custom collector
 * TICKET-ADV036 — P&L per instrument: stream reduction
 * ============================================================================
 */
@Service
public class TradeAnalyticsService {

    /** TICKET-ADV034 — count + sum of notional per counterparty. */
    public Map<Long, NotionalSummary> notionalByCounterparty(List<? extends TradeType> trades) {
        // Handle null or empty input gracefully
        if (trades == null || trades.isEmpty()) {
            return Map.of(); // Return an empty map
        }

        // Group trades by counterpartyId and compute the summary statistics in one pass
        return trades.stream()
                .collect(Collectors.groupingBy(
                        this::counterpartyIdOf,
                        Collectors.collectingAndThen(
                                Collectors.reducing(
                                        new NotionalSummary(0, BigDecimal.ZERO),
                                        trade -> {
                                            BigDecimal notional = trade.notional().amount();
                                            return new NotionalSummary(1, notional);
                                        },
                                        NotionalSummary::combine
                                ),
                                Function.identity()
                        )
                ));
    }

    /**
     * TICKET-ADV035 — VWAP = SUM(price * qty) / SUM(qty). Equity-only — only
     * EquityTrade has a meaningful price-volume pair.
     */
    public Map<String, BigDecimal> vwapByInstrument(List<EquityTrade> equityTrades) {
        if (equityTrades == null || equityTrades.isEmpty()) {
            return Map.of();
        }

        return equityTrades.stream()
            .collect(Collectors.groupingBy(EquityTrade::instrumentSymbol))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> calculateVwap(entry.getValue())
            ));
    }

    private BigDecimal calculateVwap(List<EquityTrade> trades) {
        BigDecimal totalQty = trades.stream()
            .map(EquityTrade::quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Using signum() is cleaner and faster than compareTo
        if (totalQty.signum() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedPrice = trades.stream()
            .map(t -> t.price().multiply(t.quantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return weightedPrice.divide(totalQty, 4, RoundingMode.HALF_UP);
    }

    /** TICKET-ADV036 — P&L per instrument symbol (sign by Side). */
    public Map<String, BigDecimal> pnlByInstrument(List<EquityTrade> equityTrades) {
        return equityTrades.stream()
                .collect(Collectors.groupingBy(EquityTrade::instrumentSymbol,
                        Collectors.mapping(this::pnl, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        // TICKET-ADV036: groupingBy(EquityTrade::instrumentSymbol,
        //   mapping(this::pnl, reducing(BigDecimal.ZERO, BigDecimal::add))).
        //   Side.SELL contributes positively; Side.BUY contributes negatively.
    }

    private BigDecimal pnl(EquityTrade t) {
        // TICKET-ADV036: BigDecimal abs = price * qty; SELL -> abs, BUY -> abs.negate().
        BigDecimal abs = t.price().multiply(t.quantity());
        return t.side() == com.dbtraining.reconx.model.Side.SELL ? abs : abs.negate();
    }

    private long counterpartyIdOf(TradeType t) {
        // TICKET-ADV018 — exhaustive switch over the sealed TradeType
        //   hierarchy returning t.counterpartyId() for each concrete subtype.
        return switch (t) {
            case EquityTrade et -> et.counterpartyId();
            case FXTrade ft -> ft.counterpartyId();
            case BondTrade bt -> bt.counterpartyId();
            case DerivativeTrade dt -> dt.counterpartyId();
        };
    }

    public record NotionalSummary(long count, BigDecimal total) {
        public static NotionalSummary combine(NotionalSummary a, NotionalSummary b) {
            long combinedCount = a.count + b.count;
            BigDecimal combinedTotal = a.total.add(b.total);

            return new NotionalSummary(combinedCount, combinedTotal);
        }
    }
}
