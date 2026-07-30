package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Currency;
import java.util.List;

abstract class JdbcTradeSourceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String source;

    JdbcTradeSourceRepository(JdbcTemplate jdbcTemplate, String source) {
        this.jdbcTemplate = jdbcTemplate;
        this.source = source;
    }

    protected void saveTrade(EquityTrade equityTrade) {
        jdbcTemplate.update("""
                INSERT INTO recon_trade_inputs
                    (source, trade_ref, instrument_symbol, quantity, price, currency, side, trade_date, counterparty_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                source,
                equityTrade.tradeRef().value(),
                equityTrade.instrumentSymbol(),
                equityTrade.quantity(),
                equityTrade.price(),
                equityTrade.currency().getCurrencyCode(),
                equityTrade.side().name(),
                equityTrade.tradeDate(),
                equityTrade.counterpartyId());
    }

    protected List<EquityTrade> findAllTrades() {
        return jdbcTemplate.query("""
                SELECT trade_ref, instrument_symbol, quantity, price, currency, side, trade_date, counterparty_id
                FROM recon_trade_inputs
                WHERE source = ?
                ORDER BY id
                """,
                (resultSet, rowNumber) -> EquityTrade.builder()
                        .tradeRef(TradeRef.of(resultSet.getString("trade_ref")))
                        .instrumentSymbol(resultSet.getString("instrument_symbol"))
                        .quantity(resultSet.getBigDecimal("quantity"))
                        .price(resultSet.getBigDecimal("price"))
                        .currency(Currency.getInstance(resultSet.getString("currency")))
                        .side(Side.valueOf(resultSet.getString("side")))
                        .tradeDate(resultSet.getDate("trade_date").toLocalDate())
                        .counterpartyId(resultSet.getLong("counterparty_id"))
                        .build(),
                source);
    }
}
