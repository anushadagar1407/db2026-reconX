package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.repository.ExternalTradeRepository;
import com.dbtraining.reconx.repository.InternalTradeRepository;
import com.dbtraining.reconx.repository.JdbcReconResultRepository;
import com.dbtraining.reconx.repository.ReconResultRepository;
import com.dbtraining.reconx.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class ReconciliationIntegrationTest {

    @Autowired
    private InternalTradeRepository internalTradeRepository;

    @Autowired
    private ExternalTradeRepository externalTradeRepository;

    @Autowired
    private ReconResultRepository reconResultRepository;

    @Autowired
    private ReconciliationService reconciliationService;

    @MockitoSpyBean
    private JdbcReconResultRepository jdbcReconResultRepository;

    @Test
    @Transactional
    void insertedTradesAreReconciledAndPersisted() {
        EquityTrade internal = equityTrade();
        EquityTrade external = equityTrade();

        internalTradeRepository.save(internal);
        externalTradeRepository.save(external);

        List<EquityTrade> persistedInternal = internalTradeRepository.findAll();
        List<EquityTrade> persistedExternal = externalTradeRepository.findAll();

        assertThat(persistedInternal).hasSize(1);
        assertThat(persistedExternal).hasSize(1);
        assertEquityTradeRoundTrip(persistedInternal.get(0), internal);
        assertEquityTradeRoundTrip(persistedExternal.get(0), external);

        reconciliationService.runRecon(
                internalTradeRepository.findAll(),
                externalTradeRepository.findAll(),
                ReconciliationRule.EXACT);

        List<ReconResult> persisted = reconResultRepository.findAll();

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("EQU-20260730-0001");
        assertThat(persisted.get(0).discrepancyType()).isNull();
        assertThat(persisted.get(0).details()).isNull();
    }

    @Test
    @Transactional
    void mismatchedTradesPersistBreakDetails() {
        EquityTrade internal = equityTrade("100.00");
        EquityTrade external = equityTrade("101.00");

        internalTradeRepository.save(internal);
        externalTradeRepository.save(external);

        reconciliationService.runRecon(
                internalTradeRepository.findAll(),
                externalTradeRepository.findAll(),
                ReconciliationRule.EXACT);

        List<ReconResult> persisted = reconResultRepository.findAll();

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("EQU-20260730-0001");
        assertThat(persisted.get(0).discrepancyType()).isEqualTo("VALUE_MISMATCH");
        assertThat(persisted.get(0).details())
                .contains("internal=")
                .contains("external=");
    }

    @Test
    void failedResultWriteRollsBackEarlierResults() {
        EquityTrade firstInternal = equityTrade("EQU-20260730-0001", "100.00");
        EquityTrade firstExternal = equityTrade("EQU-20260730-0001", "100.00");
        EquityTrade secondInternal = equityTrade("EQU-20260730-0002", "100.00");
        EquityTrade secondExternal = equityTrade("EQU-20260730-0002", "100.00");
        AtomicInteger writes = new AtomicInteger();

        doAnswer(invocation -> saveFirstResultThenFail(invocation, writes))
                .when(jdbcReconResultRepository)
                .save(any(ReconResult.class));

        try {
            assertThatThrownBy(() -> reconciliationService.runRecon(
                    List.of(firstInternal, secondInternal),
                    List.of(firstExternal, secondExternal),
                    ReconciliationRule.EXACT))
                    .isInstanceOf(DataAccessResourceFailureException.class);
        } finally {
            reset(jdbcReconResultRepository);
        }

        assertThat(reconResultRepository.findAll()).isEmpty();
    }

    private EquityTrade equityTrade() {
        return equityTrade("EQU-20260730-0001", "245.50");
    }

    private EquityTrade equityTrade(String price) {
        return equityTrade("EQU-20260730-0001", price);
    }

    private EquityTrade equityTrade(String ref, String price) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal(price))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 30))
                .counterpartyId(1L)
                .build();
    }

    private Object saveFirstResultThenFail(InvocationOnMock invocation, AtomicInteger writes) throws Throwable {
        if (writes.incrementAndGet() == 2) {
            throw new DataAccessResourceFailureException("forced result write failure");
        }
        return invocation.callRealMethod();
    }

    private void assertEquityTradeRoundTrip(EquityTrade actual, EquityTrade expected) {
        assertThat(actual.tradeRef()).isEqualTo(expected.tradeRef());
        assertThat(actual.instrumentSymbol()).isEqualTo(expected.instrumentSymbol());
        assertThat(actual.quantity()).isEqualByComparingTo(expected.quantity());
        assertThat(actual.price()).isEqualByComparingTo(expected.price());
        assertThat(actual.currency()).isEqualTo(expected.currency());
        assertThat(actual.side()).isEqualTo(expected.side());
        assertThat(actual.tradeDate()).isEqualTo(expected.tradeDate());
        assertThat(actual.counterpartyId()).isEqualTo(expected.counterpartyId());
    }
}
