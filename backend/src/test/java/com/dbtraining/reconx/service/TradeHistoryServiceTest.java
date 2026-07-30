package com.dbtraining.reconx.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;

@DataJpaTest
@Import(TradeHistoryService.class)
@DirtiesContext
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TradeHistoryServiceTest {

    @Autowired
    private TradeHistoryService historyService;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private CounterpartyRepository counterpartyRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recordsEveryCommittedChangeAndReturnsHistoricalSnapshots() {
        Long tradeId = transactionTemplate.execute(status -> {
            Trade trade = new Trade();
            trade.setTradeRef("TRD-ADV052-TEST");
            trade.setInstrument(instrumentRepository.findAll().getFirst());
            trade.setCounterparty(counterpartyRepository.findAll().getFirst());
            trade.setAssetClass("EQUITY");
            trade.setSide("BUY");
            trade.setQuantity(new BigDecimal("10.0000"));
            trade.setPrice(new BigDecimal("100.0000"));
            trade.setTradeDate(LocalDate.of(2026, 7, 30));
            return tradeRepository.saveAndFlush(trade).getId();
        });

        updateStatus(tradeId, TradeStatus.MATCHED);
        updateStatus(tradeId, TradeStatus.UNMATCHED);
        updateStatus(tradeId, TradeStatus.DISPUTED);

        List<Number> revisions = historyService.revisionsFor(tradeId);

        assertThat(revisions).hasSize(4);
        assertThat(historyService.snapshotAt(tradeId, revisions.get(0)).getStatus())
                .isEqualTo(TradeStatus.PENDING);
        assertThat(historyService.snapshotAt(tradeId, revisions.get(3)).getStatus())
                .isEqualTo(TradeStatus.DISPUTED);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trades_aud where id = ?",
                Long.class,
                tradeId)).isEqualTo(4L);
        assertThat(jdbcTemplate.queryForList(
                "select rev from revinfo where rev in (select rev from trades_aud where id = ?)",
                Integer.class,
                tradeId)).hasSize(4);
    }

    private void updateStatus(Long tradeId, TradeStatus newStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            Trade trade = tradeRepository.findById(tradeId).orElseThrow();
            trade.setStatus(newStatus);
            tradeRepository.saveAndFlush(trade);
        });
    }
}
