package com.dbtraining.reconx.service;

import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradeService service = new TradeService(
            tradeRepository,
            mock(CounterpartyRepository.class),
            mock(InstrumentRepository.class),
            mock(TradeEventProducer.class),
            mock(TradeMetrics.class));

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
}
