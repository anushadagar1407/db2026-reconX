package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.forCounterparty;
import static com.dbtraining.reconx.repository.TradeSpecifications.hasStatus;
import static com.dbtraining.reconx.repository.TradeSpecifications.refLike;
import static com.dbtraining.reconx.repository.TradeSpecifications.tradeDateBetween;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TradeSpecificationsTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void dateRangeSpecReturnsOnlyTradesInsideBothBounds() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        Page<Trade> page = tradeRepository.findAll(
                tradeDateBetween(from, to),
                PageRequest.of(0, 1000, Sort.by("id")));

        assertThat(page.getContent())
                .isNotEmpty()
                .allSatisfy(trade -> assertThat(trade.getTradeDate()).isBetween(from, to));
    }

    @Test
    void dateRangeSpecSupportsEitherMissingBoundary() {
        LocalDate firstDate = tradeRepository.findAll(
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "tradeDate")))
                .getContent()
                .getFirst()
                .getTradeDate();
        LocalDate lastDate = tradeRepository.findAll(
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "tradeDate")))
                .getContent()
                .getFirst()
                .getTradeDate();

        Page<Trade> throughFirstDate = tradeRepository.findAll(
                tradeDateBetween(null, firstDate), PageRequest.of(0, 1000));
        Page<Trade> fromLastDate = tradeRepository.findAll(
                tradeDateBetween(lastDate, null), PageRequest.of(0, 1000));

        assertThat(throughFirstDate.getContent())
                .isNotEmpty()
                .allSatisfy(trade -> assertThat(trade.getTradeDate()).isBeforeOrEqualTo(firstDate));
        assertThat(fromLastDate.getContent())
                .isNotEmpty()
                .allSatisfy(trade -> assertThat(trade.getTradeDate()).isAfterOrEqualTo(lastDate));
    }

    @Test
    void allFourSpecsComposeToReturnMatchingRows() {
        Trade anchor = tradeRepository.findByTradeRef("TRD-2026-000001").orElseThrow();
        String referencePrefix = anchor.getTradeRef().substring(0, anchor.getTradeRef().length() - 2);

        Specification<Trade> specification = Specification
                .where(tradeDateBetween(anchor.getTradeDate(), anchor.getTradeDate()))
                .and(hasStatus(anchor.getStatus()))
                .and(forCounterparty(anchor.getCounterparty().getId()))
                .and(refLike(referencePrefix));

        Page<Trade> page = tradeRepository.findAll(specification, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .isNotEmpty()
                .allSatisfy(trade -> {
                    assertThat(trade.getTradeDate()).isEqualTo(anchor.getTradeDate());
                    assertThat(trade.getStatus()).isEqualTo(anchor.getStatus());
                    assertThat(trade.getCounterparty().getId())
                            .isEqualTo(anchor.getCounterparty().getId());
                    assertThat(trade.getTradeRef()).startsWith(referencePrefix);
                });
        assertThat(page.getContent()).extracting(Trade::getTradeRef)
                .contains(anchor.getTradeRef());
    }

    @Test
    void nullAndBlankFiltersAreNoOps() {
        long totalTrades = tradeRepository.count();
        Specification<Trade> noFilters = Specification
                .where(tradeDateBetween(null, null))
                .and(hasStatus(null))
                .and(forCounterparty(null))
                .and(refLike("   "));

        Page<Trade> page = tradeRepository.findAll(noFilters, PageRequest.of(0, 1000));

        assertThat(page.getTotalElements()).isEqualTo(totalTrades);
    }

    @Test
    void specificationPageFetchesAssociationsNeededByTheResponseMapper() {
        Page<Trade> page = tradeRepository.findAll(
                tradeDateBetween(null, null), PageRequest.of(0, 1));

        Trade trade = page.getContent().getFirst();
        assertThat(Hibernate.isInitialized(trade.getInstrument())).isTrue();
        assertThat(Hibernate.isInitialized(trade.getCounterparty())).isTrue();
    }
}
