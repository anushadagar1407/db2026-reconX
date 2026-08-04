package com.dbtraining.reconx.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================================
 * TICKET-ADV130 — TradeEvent payload (Kafka envelope)
 *
 * WHAT:    Wire format for trade-events Kafka topic. eventId is the
 *          idempotency key; consumers deduplicate by it.
 * HOW:     Record — Jackson serialises automatically (component model
 *          = default). before/after are JSON strings (not objects) to keep
 *          the contract resilient to entity refactors.
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
        String before,
        String after
) {
    public enum EventType {
        TRADE_CREATED, TRADE_UPDATED, TRADE_CANCELLED
    }

    public static TradeEvent created(String tradeRef) {
        return created(tradeRef, null, null);
    }

    public static TradeEvent created(String tradeRef, String actor, String after) {
        return new TradeEvent(UUID.randomUUID(),
                            tradeRef,
                            EventType.TRADE_CREATED,
                            Instant.now(),
                            actor,
                            null,
                            after);
    }

    public static TradeEvent updated(String tradeRef) {
        return updated(tradeRef, null, null, null);
    }

    public static TradeEvent updated(String tradeRef, String actor,
                                     String before, String after) {
        return new TradeEvent(UUID.randomUUID(),
                            tradeRef,
                            EventType.TRADE_UPDATED,
                            Instant.now(),
                            actor,
                            before,
                            after);
    }

    public static TradeEvent cancelled(String tradeRef) {
        return cancelled(tradeRef, null, null);
    }

    public static TradeEvent cancelled(String tradeRef, String actor, String before) {
        return new TradeEvent(UUID.randomUUID(),
                            tradeRef,
                            EventType.TRADE_CANCELLED,
                            Instant.now(),
                            actor,
                            before,
                            null);
    }
}
