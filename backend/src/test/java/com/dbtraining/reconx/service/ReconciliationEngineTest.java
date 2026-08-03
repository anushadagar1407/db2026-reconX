package com.dbtraining.reconx.service;

import com.dbtraining.reconx.config.ReconConfig;
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.CacheManager;

/**
 * TICKET-ADV040 / ADV041 / ADV042 — TDD: write the test FIRST, then the impl.
 */
class ReconciliationEngineTest {

    CacheManager cacheManagerMock = mock(CacheManager.class);
    ReconConfig reconConfig = new ReconConfig(cacheManagerMock);
    private final ReconciliationEngine engine = new ReconciliationEngine(new SimpleMeterRegistry(), reconConfig);

    @Test
    @DisplayName("Reconcile exact match returns MATCHED")
    void testReconcile_exactMatch_returnsMatched() {
        // TODO(TICKET-ADV040): two identical EquityTrades + EXACT rule -> one ReconResult with status MATCHED.
        // Given: two identical EquityTrades and an EXACT rule
        EquityTrade internalTrade = equity("ABC-20260729-1234", "100.00", "10");
        EquityTrade externalTrade = equity("ABC-20260729-1234", "100.00", "10");
        ReconciliationRule exactRule = ReconciliationRule.EXACT;

        // When: reconcile is called
        List<ReconResult> results = engine.reconcile(List.of(internalTrade), List.of(externalTrade), exactRule);

        // Then: the result contains one MATCHED ReconResult
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);

    }

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance -> MATCHED")
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    @DisplayName("Reconcile price differences within 1% tolerance")
    void testReconcile_priceTolerance_withinThreshold(String priceDiff) {
        // TODO(TICKET-ADV041): prices 100.00 vs 100.50 + PRICE_TOLERANCE_1PCT rule -> status MATCHED.
        // Given: two EquityTrades with a price difference within the 1% tolerance
        BigDecimal diff = new BigDecimal(priceDiff);
        BigDecimal basePrice = new BigDecimal("100.00");
        EquityTrade internalTrade = equity("ABC-20260729-1234", String.valueOf(basePrice), "10");
        EquityTrade externalTrade = equity("ABC-20260729-1234", String.valueOf(basePrice.add(diff)), "10");
        ReconciliationRule toleranceRule = ReconciliationRule.PRICE_TOLERANCE_1PCT;

        // When: reconcile is called
        List<ReconResult> results = engine.reconcile(List.of(internalTrade), List.of(externalTrade), toleranceRule);

        // Then: the result contains one MATCHED ReconResult
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);

    }

    @Test
    @DisplayName("Reconcile missing counterparty trade returns BREAK with reason MISSING_EXTERNAL")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // TODO(TICKET-ADV042): internal trade with no external counterpart -> status BREAK,
        //                     discrepancyType = "MISSING_EXTERNAL".
        // Given: one internal EquityTrade and an empty external list
        EquityTrade internalTrade = equity("ABC-20260729-1234", "100.00", "10");
        List<TradeType> internalTrades = List.of(internalTrade);
        List<TradeType> externalTrades = List.of();
        ReconciliationRule exactRule = ReconciliationRule.EXACT;

        // When: reconcile is called
        List<ReconResult> results = engine.reconcile(internalTrades, externalTrades, exactRule);

        // Then: the result contains one BREAK ReconResult with reason MISSING_EXTERNAL
        assertThat(results).hasSize(1);
        ReconResult result = results.get(0);
        assertThat(result.status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(result.discrepancyType()).isEqualTo("MISSING_EXTERNAL");


    }

    @Test
    void testReconcile_emptyInternal_returnsEmpty() {
        // TODO(TICKET-ADV040): empty internal + empty external -> reconcile returns an empty list.
        //org.junit.jupiter.api.Assertions.fail("TICKET-ADV040 not implemented yet");

        List<TradeType> internalTrades = List.of();
        List<TradeType> externalTrades = List.of();
        ReconciliationRule exactRule = ReconciliationRule.EXACT;

        // When: reconcile is called
        List<ReconResult> results = engine.reconcile(internalTrades, externalTrades, exactRule);

        // Then: the result contains no elements
        assertThat(results).hasSize(0);
    }

    @Test
    void testReconcile_nullInternal_returnsEmpty() {
        assertThat(engine.reconcile(null, List.of(), ReconciliationRule.EXACT)).isEmpty();
    }

    @Test
    void testReconcile_nullExternal_returnsMissingExternalBreak() {
        EquityTrade internalTrade = equity("EQU-20260603-0001", "100.00", "10");

        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade), null, ReconciliationRule.EXACT);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(results.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void testReconcile_allMismatched_returnsBreakSummary() {
        List<TradeType> internalTrades = List.of(
                equity("EQU-20260603-0001", "100.00", "1000"),
                equity("EQU-20260603-0002", "100.00", "1000"),
                equity("EQU-20260603-0003", "100.00", "1000"));
        List<TradeType> externalTrades = List.of(
                equity("EQU-20260603-0001", "200.00", "1000"),
                equity("EQU-20260603-0002", "200.00", "1000"),
                equity("EQU-20260603-0003", "200.00", "1000"));

        List<ReconResult> results = engine.reconcile(
                internalTrades, externalTrades, ReconciliationRule.EXACT);
        ReconSummary summary = results.stream().collect(new ReconSummaryCollector());

        assertThat(results).hasSize(3)
                .allSatisfy(result -> assertThat(result.status()).isEqualTo(ReconResult.Status.BREAK));
        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(3);
    }

    @Test
    void testReconcile_duplicateExternalRefs_keepsFirstTrade() {
        EquityTrade internalTrade = equity("EQU-20260603-0004", "100.00", "10");
        EquityTrade firstExternalTrade = equity("EQU-20260603-0004", "100.00", "10");
        EquityTrade duplicateExternalTrade = equity("EQU-20260603-0004", "200.00", "10");

        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                List.of(firstExternalTrade, duplicateExternalTrade),
                ReconciliationRule.EXACT);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
