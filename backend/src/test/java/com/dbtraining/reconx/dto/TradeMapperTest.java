package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeMapperTest {

    private AnnotationConfigApplicationContext context;
    private TradeMapper mapper;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.scan("com.dbtraining.reconx.dto");
        context.refresh();
        mapper = context.getBean(TradeMapper.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void exposesGeneratedMapperAsSpringBean() {
        assertThat(mapper).isNotNull();
        assertThat(mapper.getClass().getName()).endsWith("TradeMapperImpl");
    }

    @Test
    void mapsEntityRelationshipsAndStatusToFlatResponse() {
        Trade trade = mock(Trade.class);
        Instrument instrument = mock(Instrument.class);
        Counterparty counterparty = mock(Counterparty.class);
        Instant createdAt = Instant.parse("2026-07-30T10:15:30Z");
        Instant modifiedAt = Instant.parse("2026-07-30T11:20:45Z");
        BigDecimal quantity = new BigDecimal("125.5000");
        BigDecimal price = new BigDecimal("98.7500");

        when(trade.getId()).thenReturn(42L);
        when(trade.getTradeRef()).thenReturn("TRD-20260730-0001");
        when(trade.getInstrument()).thenReturn(instrument);
        when(instrument.getId()).thenReturn(7L);
        when(instrument.getSymbol()).thenReturn("ACME");
        when(trade.getCounterparty()).thenReturn(counterparty);
        when(counterparty.getId()).thenReturn(9L);
        when(counterparty.getName()).thenReturn("Acme Capital");
        when(trade.getAssetClass()).thenReturn("EQUITY");
        when(trade.getSide()).thenReturn("BUY");
        when(trade.getQuantity()).thenReturn(quantity);
        when(trade.getPrice()).thenReturn(price);
        when(trade.getTradeDate()).thenReturn(LocalDate.of(2026, 7, 30));
        when(trade.getStatus()).thenReturn(TradeStatus.MATCHED);
        when(trade.getCreatedAt()).thenReturn(createdAt);
        when(trade.getModifiedAt()).thenReturn(modifiedAt);

        TradeResponse response = mapper.toResponse(trade);

        assertThat(response).isEqualTo(new TradeResponse(
                42L,
                "TRD-20260730-0001",
                7L,
                "ACME",
                9L,
                "Acme Capital",
                "EQUITY",
                "BUY",
                quantity,
                price,
                LocalDate.of(2026, 7, 30),
                "MATCHED",
                createdAt,
                modifiedAt
        ));
    }

    @Test
    void mapsRequestFieldsAndLeavesServiceManagedStateUnset() {
        TradeRequest request = new TradeRequest(
                "TRD-20260730-0002",
                7L,
                9L,
                "EQUITY",
                "SELL",
                new BigDecimal("10.2500"),
                new BigDecimal("101.1250"),
                LocalDate.of(2026, 7, 29)
        );

        Trade entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTradeRef()).isEqualTo("TRD-20260730-0002");
        assertThat(entity.getInstrument()).isNull();
        assertThat(entity.getCounterparty()).isNull();
        assertThat(entity.getAssetClass()).isEqualTo("EQUITY");
        assertThat(entity.getSide()).isEqualTo("SELL");
        assertThat(entity.getQuantity()).isEqualByComparingTo("10.2500");
        assertThat(entity.getPrice()).isEqualByComparingTo("101.1250");
        assertThat(entity.getTradeDate()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(entity.getStatus()).isEqualTo(TradeStatus.PENDING);
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getModifiedAt()).isNull();
    }

    @Test
    void handlesNullInputsAndOptionalResponseValues() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();

        Trade trade = mock(Trade.class);
        when(trade.getStatus()).thenReturn(null);

        TradeResponse response = mapper.toResponse(trade);

        assertThat(response.status()).isNull();
        assertThat(response.instrumentId()).isNull();
        assertThat(response.counterpartyId()).isNull();
    }
}
