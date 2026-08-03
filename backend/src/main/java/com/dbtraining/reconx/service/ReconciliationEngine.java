package com.dbtraining.reconx.service;

import com.dbtraining.reconx.config.ReconConfig;
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV033 — ReconciliationEngine using Streams (parallel matching)
 * TICKET-ADV037 — CompletableFuture: parallel recon by counterparty
 * TICKET-ADV047 — Edge cases: empty/single/all-mismatched inputs handled
 * TICKET-ADV084 — exports reconciliation_duration_seconds histogram
 *
 * WHAT:    Compares internal trades against external (counterparty) trades and
 *          returns a ReconResult per internal trade (MATCHED or BREAK).
 * HOW:     Index externals by tradeRef, then stream internals and look each
 *          up. CompletableFuture variant batches by counterparty for
 *          throughput on large books.
 * WHY:     This is the spine of the product. Everything else (REST API,
 *          Kafka consumers, dashboard) ultimately calls into here.
 * OBSERVE: Histogram appears at /actuator/prometheus under
 *          reconciliation_duration_seconds.
 * ============================================================================
 */
@Service
public class ReconciliationEngine {

    private final Timer reconciliationTimer;
    private final ReconConfig reconConfig;

    public ReconciliationEngine(MeterRegistry meterRegistry, ReconConfig reconConfig) {
        this.reconciliationTimer = Timer.builder("reconciliation_duration")
                .description("Time spent reconciling internal and external trades")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.reconConfig = reconConfig;
    }

    public List<ReconResult> reconcile(List<TradeType> internal,
                                       List<TradeType> external,
                                       ReconciliationRule rule) {
        return reconciliationTimer.record(() -> reconcileBatch(internal, external, rule));
    }

    private List<ReconResult> reconcileBatch(List<TradeType> internal,
                                             List<TradeType> external,
                                             ReconciliationRule rule) {
        // TICKET-ADV033: build a Map<tradeRef, TradeType> from `external`
        //   (O(1) lookups beat O(n*m) nested iteration), then parallelStream
        //   over `internal` and call matchOne(in, externalByRef.get(...), rule)
        //   for each. Guard against null/empty inputs (TICKET-ADV047).
        //   HINT:
        //     Map<String, TradeType> externalByRef = external.stream()
        //         .collect(Collectors.toMap(t -> t.tradeRef().value(), Function.identity(), (a, b) -> a));
        //     return internal.parallelStream()
        //         .map(in -> matchOne(in, externalByRef.get(in.tradeRef().value()), rule))
        //         .toList();
        // With no internal trades there is nothing to reconcile. A missing
        // external feed, however, must produce one MISSING_EXTERNAL break per
        // internal trade.
        if (internal == null || internal.isEmpty()) {
            return List.of();
        }

        // Pre-index the external trades by tradeRef for constant-time lookups
        Map<String, TradeType> externalByRef = (external == null ? List.<TradeType>of() : external).stream()
                .collect(Collectors.toMap(
                        trade -> trade.tradeRef().value(), // Key: tradeRef
                        Function.identity(),              // Value: the trade itself
                        (a, b) -> a                       // In case of duplicates, keep the first
                ));

        // Stream over the internal trades, match each one with the external trades, and collect results into a List
        return internal.parallelStream()
                .map(trade -> matchOne(trade, externalByRef.get(trade.tradeRef().value()), rule))
                .collect(Collectors.toList());
    }

    /**
     * TICKET-ADV037 — split by counterparty, reconcile each batch concurrently,
     * combine into a single result list. Caller passes one external feed per
     * counterparty (typical real-world shape).
     */
    public CompletableFuture<List<ReconResult>> reconcileByCounterparty(
            Map<Long, List<TradeType>> internalByCp,
            Map<Long, List<TradeType>> externalByCp,
            ReconciliationRule rule) {
        // TICKET-ADV037: for each counterparty key in internalByCp launch a
        //   CompletableFuture.supplyAsync(() -> reconcile(...)). Combine via
        //   CompletableFuture.allOf(...).thenApply(v -> futures.stream()
        //       .flatMap(f -> f.join().stream()).toList()).
        List<CompletableFuture<List<ReconResult>>> futures = internalByCp.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> reconcile(
                        entry.getValue(),
                        externalByCp.getOrDefault(entry.getKey(), List.of()),
                        rule)))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .flatMap(f -> f.join().stream())
                        .toList());
    }

    private ReconResult matchOne(TradeType internal, TradeType external, ReconciliationRule rule) {
        // TICKET-ADV033: if external is null return ReconResult.breakResult(ref, "MISSING_EXTERNAL", ...).
        //   Otherwise pull priceQty() for both sides, compare via rule.matches(...),
        //   return ReconResult.matched(ref) or breakResult(ref, "VALUE_MISMATCH", details).
        String ref = internal.tradeRef().value();
        if (external == null) {
            return ReconResult.breakResult(
                    ref,
                    "MISSING_EXTERNAL",
                    "No external trade found for " + ref);
        }

        BigDecimal[] internalPair = priceQty(internal);
        BigDecimal[] externalPair = priceQty(external);

        double tol = reconConfig.getPriceTolerance();

        if (rule.matches(
                internalPair[0],
                internalPair[1],
                externalPair[0],
                externalPair[1],
                tol)) {
            return ReconResult.matched(ref);
        }

        return ReconResult.breakResult(
                ref,
                "VALUE_MISMATCH",
                "internal=%s/%s external=%s/%s".formatted(
                        internalPair[0], internalPair[1], externalPair[0], externalPair[1]));
    }

    /** TICKET-ADV018 — exhaustive switch over the sealed hierarchy. */
    private BigDecimal[] priceQty(TradeType t) {
        // switch over the sealed TradeType hierarchy
        //   (EquityTrade, FXTrade, BondTrade, DerivativeTrade) and return a
        //   BigDecimal[]{price, qty}. The compiler enforces exhaustiveness —
        //   omit a case and the build fails.
        return switch (t) {
            case EquityTrade equity ->
                    new BigDecimal[]{
                            equity.price(),
                            equity.quantity()
                    };

            case FXTrade fx ->
                    new BigDecimal[]{
                            fx.fxRate(),
                            fx.notionalCcy1()
                    };

            case BondTrade bond ->
                    new BigDecimal[]{
                            bond.couponRate(),
                            bond.faceValue()
                    };

            case DerivativeTrade derivative ->
                    new BigDecimal[]{
                            derivative.strike(),
                            derivative.quantity()
                    };
        };
    }
}
