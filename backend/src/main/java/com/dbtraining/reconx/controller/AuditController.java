package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}
 * TICKET-ADV138 — GET /api/v1/audit/trades/{tradeRef}/events
 */
@RestController
@RequestMapping("/v1/audit")
@Tag(name = "audit")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public AuditController(AuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/trades/{tradeRef}")
    @Operation(summary = "Get audit history for a trade (by tradeRef)")
    @PreAuthorize("hasAnyRole('VIEWER', 'RECON_ANALYST', 'ADMIN')")
    public List<AuditLogEntry> history(@PathVariable String tradeRef) {
        // TODO(TICKET-ADV071): return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef).
        //   Day-0 returns an empty list so the React audit-trail panel renders
        //   "no history yet" instead of erroring.
        return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
    }

    @GetMapping("/trades/{tradeRef}/events")
    @Operation(summary = "Stream of all Kafka-sourced events for a trade")
    @PreAuthorize("hasAnyRole('RECON_ANALYST', 'ADMIN')")
    public List<TradeEvent> events(@PathVariable String tradeRef) {
        return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef).stream()
                .filter(AuditController::isTradeEvent)
                .map(this::toTradeEvent)
                .toList();
    }

    private static boolean isTradeEvent(AuditLogEntry entry) {
        try {
            TradeEvent.EventType.valueOf(entry.getEventType());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private TradeEvent toTradeEvent(AuditLogEntry entry) {
        return new TradeEvent(
                UUID.fromString(entry.getEventId()),
                entry.getTradeRef(),
                TradeEvent.EventType.valueOf(entry.getEventType()),
                entry.getEventTimestamp(),
                entry.getActor(),
                parseSnapshot(entry.getBeforeState(), "before", entry.getEventId()),
                parseSnapshot(entry.getAfterState(), "after", entry.getEventId()));
    }

    private JsonNode parseSnapshot(String storedJson, String field, String eventId) {
        if (storedJson == null) {
            return null;
        }
        if (storedJson.isBlank()) {
            throw invalidSnapshot(field, eventId, null);
        }
        try {
            JsonNode snapshot = objectMapper.readTree(storedJson);
            if (snapshot == null) {
                throw invalidSnapshot(field, eventId, null);
            }
            return snapshot;
        } catch (JsonProcessingException exception) {
            throw invalidSnapshot(field, eventId, exception);
        }
    }

    private static IllegalStateException invalidSnapshot(
            String field, String eventId, Exception cause) {
        return new IllegalStateException(
                "Invalid " + field + "-state JSON for eventId=" + eventId, cause);
    }
}
