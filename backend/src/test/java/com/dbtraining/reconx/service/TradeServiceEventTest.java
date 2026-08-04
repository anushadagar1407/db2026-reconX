package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TradeServiceEventTest {

    private final TradeRepository trades = mock(TradeRepository.class);
    private final CounterpartyRepository counterparties = mock(CounterpartyRepository.class);
    private final InstrumentRepository instruments = mock(InstrumentRepository.class);
    private final TradeMetrics metrics = mock(TradeMetrics.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TradeService service = new TradeService(
            trades,
            counterparties,
            instruments,
            metrics,
            events,
            new ObjectMapper().findAndRegisterModules());

    @Test
    void createRaisesCompleteAfterSnapshotWithActorOnlyAfterFlush() {
        TradeRequest request = request("TRD-CREATE", "100.0000", "245.5000");
        Instrument instrument = instrument("SAP.DE");
        Counterparty counterparty = counterparty("Deutsche Bank");
        when(trades.findByTradeRef("TRD-CREATE")).thenReturn(Optional.empty());
        when(instruments.findById(1L)).thenReturn(Optional.of(instrument));
        when(counterparties.findById(2L)).thenReturn(Optional.of(counterparty));
        when(trades.saveAndFlush(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request, "creator@db.com");

        TradeEvent event = publishedEvent();
        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CREATED);
        assertThat(event.tradeRef()).isEqualTo("TRD-CREATE");
        assertThat(event.actor()).isEqualTo("creator@db.com");
        assertThat(event.before()).isNull();
        assertThat(event.after().path("tradeRef").asText()).isEqualTo("TRD-CREATE");
        assertThat(event.after().path("instrumentSymbol").asText()).isEqualTo("SAP.DE");
        assertThat(event.after().path("counterpartyName").asText()).isEqualTo("Deutsche Bank");
        assertThat(event.after().path("quantity").decimalValue()).isEqualByComparingTo("100.0000");
        assertThat(event.after().path("price").decimalValue()).isEqualByComparingTo("245.5000");
        assertThat(event.after().path("status").asText()).isEqualTo("PENDING");

        InOrder order = inOrder(trades, events);
        order.verify(trades).saveAndFlush(any(Trade.class));
        order.verify(events).publishEvent(event);
    }

    @Test
    void fullUpdateRaisesActualBeforeAndAfterSnapshots() {
        Trade existing = trade(
                "TRD-UPDATE", "SAP.DE", "Old Counterparty", "100.0000", "245.5000", TradeStatus.PENDING);
        TradeRequest request = request("TRD-UPDATE-NEW", "125.0000", "250.2500");
        Instrument replacementInstrument = instrument("DBK.DE");
        Counterparty replacementCounterparty = counterparty("New Counterparty");
        when(trades.findById(42L)).thenReturn(Optional.of(existing));
        when(instruments.findById(1L)).thenReturn(Optional.of(replacementInstrument));
        when(counterparties.findById(2L)).thenReturn(Optional.of(replacementCounterparty));
        when(trades.saveAndFlush(existing)).thenReturn(existing);

        service.update(42L, request, "editor@db.com");

        TradeEvent event = publishedEvent();
        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(event.tradeRef()).isEqualTo("TRD-UPDATE-NEW");
        assertThat(event.actor()).isEqualTo("editor@db.com");
        assertThat(event.before().path("tradeRef").asText()).isEqualTo("TRD-UPDATE");
        assertThat(event.before().path("instrumentSymbol").asText()).isEqualTo("SAP.DE");
        assertThat(event.before().path("counterpartyName").asText()).isEqualTo("Old Counterparty");
        assertThat(event.before().path("quantity").decimalValue()).isEqualByComparingTo("100.0000");
        assertThat(event.before().path("price").decimalValue()).isEqualByComparingTo("245.5000");
        assertThat(event.after().path("tradeRef").asText()).isEqualTo("TRD-UPDATE-NEW");
        assertThat(event.after().path("instrumentSymbol").asText()).isEqualTo("DBK.DE");
        assertThat(event.after().path("counterpartyName").asText()).isEqualTo("New Counterparty");
        assertThat(event.after().path("quantity").decimalValue()).isEqualByComparingTo("125.0000");
        assertThat(event.after().path("price").decimalValue()).isEqualByComparingTo("250.2500");

        InOrder order = inOrder(trades, events);
        order.verify(trades).saveAndFlush(existing);
        order.verify(events).publishEvent(event);
    }

    @Test
    void statusUpdateRaisesSnapshotsWithOnlyStatusChanged() {
        Trade existing = trade(
                "TRD-STATUS", "SAP.DE", "Counterparty", "100.0000", "245.5000", TradeStatus.PENDING);
        when(trades.findById(42L)).thenReturn(Optional.of(existing));
        when(trades.saveAndFlush(existing)).thenReturn(existing);

        service.updateStatus(42L, "MATCHED", "status-editor@db.com");

        TradeEvent event = publishedEvent();
        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(event.actor()).isEqualTo("status-editor@db.com");
        assertThat(event.before().path("status").asText()).isEqualTo("PENDING");
        assertThat(event.after().path("status").asText()).isEqualTo("MATCHED");
        assertThat(event.before().path("quantity")).isEqualTo(event.after().path("quantity"));
        assertThat(event.before().path("price")).isEqualTo(event.after().path("price"));

        InOrder order = inOrder(trades, events);
        order.verify(trades).saveAndFlush(existing);
        order.verify(events).publishEvent(event);
    }

    @Test
    void softDeleteRaisesCancellationWithPreDeleteSnapshot() {
        Trade existing = trade(
                "TRD-CANCEL", "SAP.DE", "Counterparty", "100.0000", "245.5000", TradeStatus.MATCHED);
        when(trades.findById(42L)).thenReturn(Optional.of(existing));
        when(trades.saveAndFlush(existing)).thenReturn(existing);

        service.softDelete(42L, "admin@db.com");

        TradeEvent event = publishedEvent();
        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(event.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CANCELLED);
        assertThat(event.tradeRef()).isEqualTo("TRD-CANCEL");
        assertThat(event.actor()).isEqualTo("admin@db.com");
        assertThat(event.before().path("status").asText()).isEqualTo("MATCHED");
        assertThat(event.before().path("deletedAt").isNull()).isTrue();
        assertThat(event.after()).isNull();

        InOrder order = inOrder(trades, events);
        order.verify(trades).saveAndFlush(existing);
        order.verify(events).publishEvent(event);
    }

    @Test
    void failedPersistenceDoesNotRaiseAnEvent() {
        TradeRequest request = request("TRD-FAILED", "100.0000", "245.5000");
        when(trades.findByTradeRef("TRD-FAILED")).thenReturn(Optional.empty());
        when(instruments.findById(1L)).thenReturn(Optional.of(instrument("SAP.DE")));
        when(counterparties.findById(2L)).thenReturn(Optional.of(counterparty("Counterparty")));
        when(trades.saveAndFlush(any(Trade.class))).thenThrow(new IllegalStateException("write failed"));

        assertThatThrownBy(() -> service.create(request, "creator@db.com"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(events);
    }

    private TradeEvent publishedEvent() {
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(TradeEvent.class);
        return (TradeEvent) event.getValue();
    }

    private static TradeRequest request(String tradeRef, String quantity, String price) {
        return new TradeRequest(
                tradeRef,
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal(quantity),
                new BigDecimal(price),
                LocalDate.of(2026, 7, 30));
    }

    private static Trade trade(
            String tradeRef,
            String instrumentSymbol,
            String counterpartyName,
            String quantity,
            String price,
            TradeStatus status) {
        Trade trade = new Trade();
        trade.setTradeRef(tradeRef);
        trade.setInstrument(instrument(instrumentSymbol));
        trade.setCounterparty(counterparty(counterpartyName));
        trade.setAssetClass("EQUITY");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal(quantity));
        trade.setPrice(new BigDecimal(price));
        trade.setTradeDate(LocalDate.of(2026, 7, 30));
        trade.setStatus(status);
        return trade;
    }

    private static Instrument instrument(String symbol) {
        Instrument instrument = new Instrument();
        instrument.setSymbol(symbol);
        return instrument;
    }

    private static Counterparty counterparty(String name) {
        Counterparty counterparty = new Counterparty();
        counterparty.setName(name);
        return counterparty;
    }
}
