package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceSoftDeleteTest {

    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradeService service = new TradeService(
            tradeRepository,
            mock(CounterpartyRepository.class),
            mock(InstrumentRepository.class),
            mock(TradeMetrics.class),
            mock(ApplicationEventPublisher.class),
            new ObjectMapper().findAndRegisterModules());

    @Test
    void softDeleteMarksAndSavesTheLoadedTrade() {
        Trade trade = new Trade();
        when(tradeRepository.findById(42L)).thenReturn(Optional.of(trade));

        service.softDelete(42L, "delete-actor");

        assertThat(trade.getDeletedAt()).isNotNull();
        verify(tradeRepository).saveAndFlush(trade);
    }

    @Test
    void softDeleteRaisesNotFoundForMissingTrade() {
        when(tradeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(999L, "delete-actor"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessage("id=999");
    }

    @Test
    void repeatedSoftDeleteReturnsNotFoundWithoutSavingAgain() {
        Trade trade = new Trade();
        when(tradeRepository.findById(42L))
                .thenReturn(Optional.of(trade), Optional.empty());

        service.softDelete(42L, "delete-actor");

        assertThatThrownBy(() -> service.softDelete(42L, "delete-actor"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessage("id=42");
        assertThat(trade.getDeletedAt()).isNotNull();
        verify(tradeRepository, times(1)).saveAndFlush(trade);
    }
}
