package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
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

    public AuditController(AuditLogRepository auditRepo) { this.auditRepo = auditRepo; }

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
                .map(AuditController::toTradeEvent)
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

    private static TradeEvent toTradeEvent(AuditLogEntry entry) {
        return new TradeEvent(
                UUID.fromString(entry.getEventId()),
                entry.getTradeRef(),
                TradeEvent.EventType.valueOf(entry.getEventType()),
                entry.getEventTimestamp(),
                entry.getActor(),
                entry.getBeforeState(),
                entry.getAfterState());
    }
}
