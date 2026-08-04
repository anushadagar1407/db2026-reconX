package com.dbtraining.reconx.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================================
 * TICKET-ADV130 — TradeEvent payload (Kafka envelope)
 *
 * WHAT:    Wire format for trade-events Kafka topic. eventId is the
 *          idempotency key; consumers deduplicate by it.
 * HOW:     Record — Jackson serialises automatically. before/after are DTO
 *          snapshots represented as JSON trees, never JPA entities.
 * WHY:     Including before+after on every event makes downstream consumers
 *          (audit, recon) self-contained — they don't have to fetch the
 *          current state from the DB.
 * ============================================================================
 */
public record TradeEvent(
        UUID eventId,
        String tradeRef,
        EventType eventType,
        Instant timestamp,
        String actor,
        JsonNode before,
        JsonNode after
) {
    public enum EventType {
        TRADE_CREATED, TRADE_UPDATED, TRADE_CANCELLED
    }

    public static TradeEvent created(String tradeRef, JsonNode after) {
        return created(tradeRef, null, after);
    }

    public static TradeEvent created(String tradeRef, String actor, JsonNode after) {
        return event(tradeRef, EventType.TRADE_CREATED, actor, null, after);
    }

    public static TradeEvent updated(String tradeRef, JsonNode before, JsonNode after) {
        return updated(tradeRef, null, before, after);
    }

    public static TradeEvent updated(String tradeRef, String actor, JsonNode before, JsonNode after) {
        return event(tradeRef, EventType.TRADE_UPDATED, actor, before, after);
    }

    public static TradeEvent cancelled(String tradeRef, JsonNode before) {
        return cancelled(tradeRef, null, before);
    }

    public static TradeEvent cancelled(String tradeRef, String actor, JsonNode before) {
        return event(tradeRef, EventType.TRADE_CANCELLED, actor, before, null);
    }

    private static TradeEvent event(
            String tradeRef,
            EventType eventType,
            String actor,
            JsonNode before,
            JsonNode after) {
        return new TradeEvent(UUID.randomUUID(),
                tradeRef,
                eventType,
                Instant.now(),
                actor,
                before,
                after);
    }
}
