package com.dbtraining.reconx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * ============================================================================
 * TICKET-ADV053 — TradeRequest DTO (POST body) TICKET-ADV029 — JSR-380
 * validation annotations live on the DTO, not the entity
 *
 * WHY: Putting @Pattern/@Positive/@NotNull on the JPA entity couples
 * persistence to wire format. The DTO is the wire contract; validate it before
 * mapping.
 * ============================================================================
 */
public record TradeRequest(
        @NotNull
        @Pattern(
                regexp = "^[A-Z]{3}-\\d{8}-\\d{4}$",
                message = "tradeRef must match AAA-YYYYMMDD-NNNN"
        )
        String tradeRef,
        @NotNull
        Long instrumentId,
        @NotNull
        Long counterpartyId,
        @NotBlank
        String assetClass,
        @NotBlank
        @Pattern(
                regexp = "^(BUY|SELL)$",
                message = "side must be BUY or SELL"
        )
        String side,
        @NotNull
        @Positive
        BigDecimal quantity,
        @NotNull
        @PositiveOrZero
        BigDecimal price,
        @NotNull
        LocalDate tradeDate
        ) {

}
