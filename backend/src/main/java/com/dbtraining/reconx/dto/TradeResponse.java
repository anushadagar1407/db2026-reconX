package com.dbtraining.reconx.dto;

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
        String status,
        Instant createdAt,
        Instant modifiedAt
        ) {

}
