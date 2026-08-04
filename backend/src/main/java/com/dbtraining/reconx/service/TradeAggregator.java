package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Rebuilds the latest trade projection by folding its ordered audit events. */
@Service
public class TradeAggregator {

    private static final Logger log = LoggerFactory.getLogger(TradeAggregator.class);

    private final AuditLogRepository auditRepository;
    private final ObjectMapper objectMapper;

    public TradeAggregator(AuditLogRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events =
                auditRepository.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;
        for (AuditLogEntry event : events) {
            switch (event.getEventType()) {
                case "TRADE_CREATED", "TRADE_UPDATED" ->
                        state = parseSnapshot(event);
                case "TRADE_CANCELLED" -> state = null;
                default -> log.debug(
                        "Ignoring non-trade audit event type={} tradeRef={} eventId={}",
                        event.getEventType(), tradeRef, event.getEventId());
            }
        }

        log.debug("Rebuilt trade state tradeRef={} eventCount={} present={}",
                tradeRef, events.size(), state != null);
        return Optional.ofNullable(state);
    }

    private JsonNode parseSnapshot(AuditLogEntry event) {
        if (event.getAfterState() == null || event.getAfterState().isBlank()) {
            throw new IllegalStateException(
                    "Missing after-state for eventId=" + event.getEventId());
        }
        try {
            return objectMapper.readTree(event.getAfterState());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Invalid after-state JSON for eventId=" + event.getEventId(), exception);
        }
    }
}
