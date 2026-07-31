package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.TradeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 *  * TICKET-ADV053 — TradeResponse DTO.
 *
 */
public record TradeResponse(
        Long id,
        String tradeRef,
        Long counterpartyId,
        String counterpartyName,
        Long instrumentId,
        String instrumentSymbol,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate,
        TradeStatus status,
        Instant createdAt,
        Instant modifiedAt
        ) {

}