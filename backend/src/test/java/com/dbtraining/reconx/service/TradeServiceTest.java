package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final CounterpartyRepository counterpartyRepository = mock(CounterpartyRepository.class);
    private final InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    private final TradeService service = new TradeService(
            tradeRepository,
            counterpartyRepository,
            instrumentRepository,
            mock(TradeEventProducer.class),
            mock(TradeMetrics.class));

    @Test
    void findByIdReturnsTrade() {
        Trade expected = new Trade();
        when(tradeRepository.findById(42L)).thenReturn(Optional.of(expected));

        assertThat(service.findById(42L)).isSameAs(expected);

        verify(tradeRepository).findById(42L);
    }

    @Test
    void findByIdRejectsMissingTrade() {
        when(tradeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessage("Trade not found: id=404");

        verify(tradeRepository).findById(404L);
    }

    @Test
    void createBuildsAndSavesPendingTrade() {
        TradeRequest request = validRequest();
        Instrument instrument = mock(Instrument.class);
        Counterparty counterparty = mock(Counterparty.class);
        when(tradeRepository.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepository.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepository.findById(request.counterpartyId())).thenReturn(Optional.of(counterparty));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trade saved = service.create(request, "trader");

        assertThat(saved.getTradeRef()).isEqualTo(request.tradeRef());
        assertThat(saved.getInstrument()).isSameAs(instrument);
        assertThat(saved.getCounterparty()).isSameAs(counterparty);
        assertThat(saved.getAssetClass()).isEqualTo(request.assetClass());
        assertThat(saved.getSide()).isEqualTo(request.side());
        assertThat(saved.getQuantity()).isEqualByComparingTo(request.quantity());
        assertThat(saved.getPrice()).isEqualByComparingTo(request.price());
        assertThat(saved.getTradeDate()).isEqualTo(request.tradeDate());
        assertThat(saved.getStatus()).isEqualTo(TradeStatus.PENDING);
        verify(tradeRepository).findByTradeRef(request.tradeRef());
        verify(tradeRepository).save(saved);
    }

    @Test
    void createRejectsDuplicateTradeReference() {
        TradeRequest request = validRequest();
        when(tradeRepository.findByTradeRef(request.tradeRef()))
                .thenReturn(Optional.of(new Trade()));

        assertThatThrownBy(() -> service.create(request, "trader"))
                .isInstanceOf(DuplicateTradeRefException.class)
                .hasMessageContaining(request.tradeRef());

        verifyNoInteractions(instrumentRepository, counterpartyRepository);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void createRejectsMissingInstrumentBeforeLookingUpCounterparty() {
        TradeRequest request = validRequest();
        when(tradeRepository.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepository.findById(request.instrumentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, "trader"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("Instrument");

        verifyNoInteractions(counterpartyRepository);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void createRejectsMissingCounterpartyBeforeSaving() {
        TradeRequest request = validRequest();
        Instrument instrument = mock(Instrument.class);
        when(tradeRepository.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepository.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepository.findById(request.counterpartyId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, "trader"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("Counterparty");

        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void createTranslatesTradeReferenceUniqueConstraintRace() {
        TradeRequest request = validRequest();
        Instrument instrument = mock(Instrument.class);
        Counterparty counterparty = mock(Counterparty.class);
        when(tradeRepository.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepository.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepository.findById(request.counterpartyId())).thenReturn(Optional.of(counterparty));
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint uk_trades_trade_ref for trade_ref");
        when(tradeRepository.save(any(Trade.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(request, "trader"))
                .isInstanceOf(DuplicateTradeRefException.class)
                .hasMessageContaining(request.tradeRef())
                .hasCause(failure);
    }

    @Test
    void createRethrowsUnrelatedIntegrityViolation() {
        TradeRequest request = validRequest();
        Instrument instrument = mock(Instrument.class);
        Counterparty counterparty = mock(Counterparty.class);
        when(tradeRepository.findByTradeRef(request.tradeRef())).thenReturn(Optional.empty());
        when(instrumentRepository.findById(request.instrumentId())).thenReturn(Optional.of(instrument));
        when(counterpartyRepository.findById(request.counterpartyId())).thenReturn(Optional.of(counterparty));
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "foreign key constraint violation for counterparty_id");
        when(tradeRepository.save(any(Trade.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(request, "trader"))
                .isSameAs(failure);
    }

    @Test
    void listComposesTypedFiltersAndPreservesPagination() {
        Pageable pageable = PageRequest.of(1, 2);
        Page<Trade> expected = new PageImpl<>(List.of(), pageable, 500);
        when(tradeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Trade> actual = service.list(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30),
                "PENDING",
                7L,
                pageable);

        assertThat(actual).isSameAs(expected);
        verify(tradeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void listRejectsStatusThatIsNotAnEntityValue() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.list(null, null, "SETTLED", null, pageable))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(tradeRepository);
    }

    private TradeRequest validRequest() {
        return new TradeRequest(
                "TRD-20260730-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.5000"),
                LocalDate.of(2026, 7, 30));
    }
}
