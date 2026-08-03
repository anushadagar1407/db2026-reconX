package com.dbtraining.reconx.service;

import com.dbtraining.reconx.config.ReconConfig;
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReconciliationEngineMetricsTest {

    @Test
    void recordsEveryReconcileInvocationAndPreservesResults() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        CacheManager cacheManagerMock = mock(CacheManager.class);
        ReconConfig reconConfig = new ReconConfig(cacheManagerMock);
        ReconciliationEngine engine = new ReconciliationEngine(registry, reconConfig);

        List<ReconResult> matched = engine.reconcile(
                List.of(equity("ABC-20260729-0001", "100.00", "10")),
                List.of(equity("ABC-20260729-0001", "100.00", "10")),
                ReconciliationRule.EXACT);
        List<ReconResult> empty = engine.reconcile(List.of(), List.of(), ReconciliationRule.EXACT);

        assertThat(matched).singleElement()
                .extracting(ReconResult::status)
                .isEqualTo(ReconResult.Status.MATCHED);
        assertThat(empty).isEmpty();

        Timer timer = registry.get("reconciliation_duration").timer();
        assertThat(timer.count()).isEqualTo(2);

        String scrape = registry.scrape();
        assertThat(scrape).contains("# HELP reconciliation_duration_seconds");
        assertThat(scrape).contains("# TYPE reconciliation_duration_seconds histogram");
        assertThat(scrape).contains("reconciliation_duration_seconds_count 2\n");
        assertThat(scrape).contains("reconciliation_duration_seconds_sum ");
        assertThat(scrape).contains("reconciliation_duration_seconds_bucket{");
        assertThat(scrape).doesNotContain("reconciliation_duration_seconds_seconds");
    }

    private EquityTrade equity(String ref, String price, String quantity) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(quantity))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
