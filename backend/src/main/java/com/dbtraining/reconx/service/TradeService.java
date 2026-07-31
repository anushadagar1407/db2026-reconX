package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;

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
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(TradeRepository tradeRepo,
            CounterpartyRepository cpRepo,
            InstrumentRepository instRepo,
            TradeEventProducer events,
            TradeMetrics metrics) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
    }

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

        return tradeRepo.save(trade);
    }

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

        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());

        Trade saved = tradeRepo.save(trade);

        events.publish(
                new TradeEvent(
                        UUID.randomUUID(),
                        saved.getTradeRef(),
                        TradeEvent.EventType.TRADE_UPDATED,
                        Instant.now(),
                        actor,
                        null,
                        saved.getStatus().name()
                )
        );

        return saved;
    }

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

        String beforeStatus = trade.getStatus() == null ? null : trade.getStatus().name();

        trade.setStatus(tradeStatus);

        Trade saved = tradeRepo.save(trade);

        events.publish(
                new TradeEvent(
                        UUID.randomUUID(),
                        saved.getTradeRef(),
                        TradeEvent.EventType.TRADE_UPDATED,
                        Instant.now(),
                        actor,
                        beforeStatus,
                        saved.getStatus().name()
                )
        );

        return saved;
    }

    public void softDelete(Long id, String actor) {
        // TODO(TICKET-ADV067): load, call t.softDelete() (sets deleted_at), save,
        //   publish a TRADE_CANCELLED event.
        throw new UnsupportedOperationException("TICKET-ADV067");
    }

    @Transactional(readOnly = true)
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
