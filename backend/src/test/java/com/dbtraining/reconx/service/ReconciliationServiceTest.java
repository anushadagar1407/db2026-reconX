package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import com.dbtraining.reconx.repository.ReconResultRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReconciliationServiceTest {

    @Test
    void testReconcile_savesResultWithMatchedStatus() {
        // GIVEN: A mocked repository, a real engine, and identical trades
        ReconResultRepository repo = mock(ReconResultRepository.class);
        ReconciliationEngine engine = new ReconciliationEngine(new SimpleMeterRegistry());
        ReconciliationService service = new ReconciliationService(engine, repo);

        String validRefString = "EQU-20260729-0001";
        
        // Clean 1-liner creation via private helper
        EquityTrade internal = equity(validRefString, "100.00", "10");
        EquityTrade external = equity(validRefString, "100.00", "10");

        // WHEN: Executing reconciliation using EXACT matching
        service.runRecon(List.of(internal), List.of(external), ReconciliationRule.EXACT);

        // THEN: Verify single MATCHED result is saved to repository
        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().tradeRef()).isEqualTo(validRefString);
        assertThat(captor.getValue().status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    // --- Local Test Helper ---
    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 29))
                .counterpartyId(101L)
                .build();
    }
}
