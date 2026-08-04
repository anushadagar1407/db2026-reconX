package com.dbtraining.reconx.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ============================================================================
 * TICKET-ADV132 — AuditEventConsumer
 *
 * WHAT:    Persists every TradeEvent flowing through `trade-events` into the
 *          audit_log table.
 * HOW:     @KafkaListener on `trade-events`, groupId `audit-service`. Maps
 *          the TradeEvent DTO -> AuditLogEntry entity -> repo.save(...).
 * WHY:     Together with ADV137 this powers event-sourced replay — every
 *          domain change is captured immutably.
 * OBSERVE: After a POST /api/v1/trades, query audit_log -> one new row with
 *          the same eventId.
 * ============================================================================
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    private final AuditLogRepository repo;

    public AuditEventConsumer(AuditLogRepository repo) { this.repo = repo; }

    @KafkaListener(
            topics = "trade-events",
            groupId = "audit-service",
            containerFactory = "tradeEventListenerContainerFactory")
    @Transactional
    public void onTradeEvent(TradeEvent e) {
        String eventId = e.eventId().toString();
        if (repo.existsByEventId(eventId)) {
            log.debug("Audit row already exists for eventId={}", eventId);
            return;
        }
        repo.save(new AuditLogEntry(
                eventId,
                e.tradeRef(),
                e.eventType().name(),
                e.timestamp(),
                e.actor(),
                json(e.before()),
                json(e.after())));
        log.debug("Audit row persisted for eventId={} tradeRef={}",
                e.eventId(), e.tradeRef());
    }

    private static String json(JsonNode snapshot) {
        return snapshot == null || snapshot.isNull() ? null : snapshot.toString();
    }
}
