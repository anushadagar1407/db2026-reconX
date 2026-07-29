package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // TODO(TICKET-ADV034): Collectors.groupingBy(this::counterpartyIdOf,
        //   Collectors.collectingAndThen(toList(), list -> new NotionalSummary(
        //       list.size(),
        //       list.stream().map(t -> t.notional().amount()).reduce(ZERO, BigDecimal::add)))).
        throw new UnsupportedOperationException("TICKET-ADV034");
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
        // TODO(TICKET-ADV036): groupingBy(EquityTrade::instrumentSymbol,
        //   mapping(this::pnl, reducing(BigDecimal.ZERO, BigDecimal::add))).
        //   Side.SELL contributes positively; Side.BUY contributes negatively.
        throw new UnsupportedOperationException("TICKET-ADV036");
    }

    private BigDecimal pnl(EquityTrade t) {
        // TODO(TICKET-ADV036): BigDecimal abs = price * qty; SELL -> abs, BUY -> abs.negate().
        throw new UnsupportedOperationException("TICKET-ADV036");
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

    public record NotionalSummary(long count, BigDecimal total) {}
}
