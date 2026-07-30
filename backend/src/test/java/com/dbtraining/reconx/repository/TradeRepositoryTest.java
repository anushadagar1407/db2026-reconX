package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TradeRepositoryTest {

    private static final LocalDate FIRST_SEED_DATE = LocalDate.of(2026, 4, 1);
    private static final LocalDate LAST_SEED_DATE = LocalDate.of(2026, 7, 29);

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private CounterpartyRepository counterpartyRepository;

    @Test
    void findByTradeRefReturnsSeededTrade() {
        assertThat(tradeRepository.findByTradeRef("TRD-2026-000001"))
                .get()
                .extracting(Trade::getTradeRef)
                .isEqualTo("TRD-2026-000001");
    }

    @Test
    void findByFiltersReturnsPagedTradesWithinDateRange() {
        Page<Trade> page = tradeRepository.findByFilters(
                FIRST_SEED_DATE,
                LAST_SEED_DATE,
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(page.getContent())
                .hasSize(10)
                .allSatisfy(trade -> assertThat(trade.getTradeDate())
                        .isBetween(FIRST_SEED_DATE, LAST_SEED_DATE));
        assertThat(page.getTotalElements()).isEqualTo(500);
    }

    @Test
    void findByFiltersAppliesStatusAndCounterparty() {
        Counterparty counterparty = counterpartyRepository
                .findByLeiCode("5493001ABCDE12345007")
                .orElseThrow();

        Page<Trade> page = tradeRepository.findByFilters(
                FIRST_SEED_DATE,
                LAST_SEED_DATE,
                TradeStatus.PENDING,
                counterparty.getId(),
                PageRequest.of(0, 100));

        assertThat(page.getContent())
                .isNotEmpty()
                .allSatisfy(trade -> {
                    assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);
                    assertThat(trade.getCounterparty().getId()).isEqualTo(counterparty.getId());
                });
    }
}
