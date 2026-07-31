package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import io.micrometer.core.instrument.Gauge;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Service;
/**
 * ============================================================================
 * TICKET-ADV064 — TradeService.create (POST endpoint backing) TICKET-ADV065 —
 * update TICKET-ADV066 — updateStatus (PATCH) TICKET-ADV067 — softDelete
 * TICKET-ADV083 — increments trade_created_total Counter on create
 * TICKET-ADV129 — publishes TradeEvent on every state change TICKET-ADV056 —
 * list() uses Specifications
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeMetrics metrics;
    private final Counter tradeCreatedCounter;


    public TradeService(TradeRepository tradeRepo,
            CounterpartyRepository cpRepo,
            InstrumentRepository instRepo,
            TradeMetrics metrics,
                        MeterRegistry meterRegistry) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.metrics = metrics;
        this.tradeCreatedCounter = meterRegistry.counter("trade_created_total");

        Gauge.builder("trade_status_count", () -> tradeRepo.countByStatus("PENDING"))
                .tag("status", "PENDING")
                .register(meterRegistry);

        Gauge.builder("trade_status_count", () -> tradeRepo.countByStatus("MATCHED"))
                .tag("status", "MATCHED")
                .register(meterRegistry);

        Gauge.builder("trade_status_count", () -> tradeRepo.countByStatus("UNMATCHED"))
                .tag("status", "UNMATCHED")
                .register(meterRegistry);

        Gauge.builder("trade_status_count", () -> tradeRepo.countByStatus("DISPUTED"))
                .tag("status", "DISPUTED")
                .register(meterRegistry);

        Gauge.builder("trade_status_count", () -> tradeRepo.countByStatus("CANCELLED"))
                .tag("status", "CANCELLED")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VIEWER', 'TRADER', 'RECON_ANALYST', 'ADMIN')")
    public Trade findById(Long id) {
        return tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade not found: id=" + id));
    }

    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public Trade create(TradeRequest req, String actor) {
        // TICKET-ADV064: reject duplicate tradeRef via DuplicateTradeRefException,
        //   build a new Trade with instrument + counterparty looked up from
        //   their repos (throw TradeNotFoundException on miss), status = "PENDING",
        //   save, then:
        //     - metrics.incrementTradeCreated() + metrics.recordTradeValue(qty*price) — TICKET-ADV083
        //     - events.publish(new TradeEvent(... TRADE_CREATED ... actor ...)) — TICKET-ADV129
        if (tradeRepo.findByTradeRef(req.tradeRef()).isPresent()) {
            throw new DuplicateTradeRefException(
                    "Trade with reference " + req.tradeRef() + " already exists");
        }

        Trade trade = new Trade();
        trade.setTradeRef(req.tradeRef());
        trade.setInstrument(instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Instrument with id " + req.instrumentId() + " not found")));
        trade.setCounterparty(cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Counterparty with id " + req.counterpartyId() + " not found")));
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());
        trade.setStatus(TradeStatus.PENDING);

        try {
            Trade savedTrade = tradeRepo.save(trade);
            tradeCreatedCounter.increment();
            return savedTrade;
        } catch (DataIntegrityViolationException ex) {
            if (!isTradeReferenceUniqueViolation(ex)) {
                throw ex;
            }
            throw new DuplicateTradeRefException(
                    "Trade with reference " + req.tradeRef() + " already exists", ex);
        }
    }

    private static boolean isTradeReferenceUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase(java.util.Locale.ROOT).contains("trade_ref")) {
                    return true;
                }
            }
            String message = cause.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("trade_ref")
                        && (normalized.contains("unique") || normalized.contains("duplicate"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public Trade update(Long id, TradeRequest req, String actor) {

        Trade trade = tradeRepo.findById(id)
                .orElseThrow(()
                        -> new TradeNotFoundException("Trade not found: id=" + id));

        trade.setTradeRef(req.tradeRef());

        trade.setInstrument(
                instRepo.findById(req.instrumentId())
                        .orElseThrow(()
                                -> new TradeNotFoundException(
                                "Instrument not found: id=" + req.instrumentId()))
        );

        trade.setCounterparty(
                cpRepo.findById(req.counterpartyId())
                        .orElseThrow(()
                                -> new TradeNotFoundException(
                                 "Counterparty not found: id=" + req.counterpartyId()))
        );

        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());

        return tradeRepo.save(trade);
    }

    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public Trade updateStatus(Long id, String status, String actor) {
        if (status == null || status.isBlank()) {
            throw new InvalidTradeException("Status is required");
        }

        TradeStatus tradeStatus;
        try {
            tradeStatus = TradeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTradeException("Invalid trade status: " + status);
        }

        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade not found: id=" + id));

        trade.setStatus(tradeStatus);

        return tradeRepo.save(trade);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void softDelete(Long id, String actor) {
        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id=" + id));
        trade.softDelete();
        tradeRepo.save(trade);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('VIEWER', 'TRADER', 'RECON_ANALYST', 'ADMIN')")
    public Page<Trade> list(LocalDate from, LocalDate to, String status, Long counterpartyId, Pageable pageable) {
        TradeStatus tradeStatus = status == null || status.isBlank()
                ? null
                : TradeStatus.valueOf(status);
        Specification<Trade> specification = Specification
                .where(tradeDateBetween(from, to))
                .and(hasStatus(tradeStatus))
                .and(forCounterparty(counterpartyId));
        return tradeRepo.findAll(specification, pageable);
    }
}
