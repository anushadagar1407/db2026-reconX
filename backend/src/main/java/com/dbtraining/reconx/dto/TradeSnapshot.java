package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TradeSnapshot(
        Long id,
        String tradeRef,
        Long instrumentId,
        String instrumentSymbol,
        Long counterpartyId,
        String counterpartyName,
        String assetClass,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate,
        String status,
        Instant deletedAt,
        Instant createdAt,
        Instant modifiedAt
) {
    public static TradeSnapshot from(Trade trade) {
        Instrument instrument = trade.getInstrument();
        Counterparty counterparty = trade.getCounterparty();
        return new TradeSnapshot(
                trade.getId(),
                trade.getTradeRef(),
                instrument == null ? null : instrument.getId(),
                instrument == null ? null : instrument.getSymbol(),
                counterparty == null ? null : counterparty.getId(),
                counterparty == null ? null : counterparty.getName(),
                trade.getAssetClass(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradeDate(),
                trade.getStatus() == null ? null : trade.getStatus().name(),
                trade.getDeletedAt(),
                trade.getCreatedAt(),
                trade.getModifiedAt());
    }
}
