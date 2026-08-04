package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditControllerTest {

    @Test
    void returnsKafkaTradeEventsInRepositoryOrder() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository, new ObjectMapper());
        String tradeRef = "TRD-138";
        UUID createdId = UUID.randomUUID();
        UUID updatedId = UUID.randomUUID();
        when(repository.findByTradeRefOrderByEventTimestampAsc(tradeRef))
                .thenReturn(List.of(
                        entry(createdId, tradeRef, "TRADE_CREATED", 1),
                        entry(UUID.randomUUID(), tradeRef, "READ", 2),
                        entry(updatedId, tradeRef, "TRADE_UPDATED", 3)));

        List<TradeEvent> events = controller.events(tradeRef);

        assertThat(events).extracting(TradeEvent::eventId)
                .containsExactly(createdId, updatedId);
        assertThat(events).extracting(TradeEvent::eventType)
                .containsExactly(
                        TradeEvent.EventType.TRADE_CREATED,
                        TradeEvent.EventType.TRADE_UPDATED);
        assertThat(events.get(0).actor()).isEqualTo("tester");
        assertThat(events.get(0).before()).isNull();
        assertThat(events.get(0).after().isObject()).isTrue();
        assertThat(events.get(0).after().path("details").path("status").asText())
                .isEqualTo("PENDING");
        assertThat(events.get(1).before().path("details").path("status").asText())
                .isEqualTo("PENDING");
        assertThat(events.get(1).after().path("details").path("status").asText())
                .isEqualTo("MATCHED");
        verify(repository).findByTradeRefOrderByEventTimestampAsc(tradeRef);
    }

    @Test
    void rejectsInvalidStoredSnapshotJsonWithEventContext() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository, new ObjectMapper());
        String tradeRef = "TRD-INVALID";
        UUID eventId = UUID.randomUUID();
        when(repository.findByTradeRefOrderByEventTimestampAsc(tradeRef))
                .thenReturn(List.of(new AuditLogEntry(
                        eventId.toString(),
                        tradeRef,
                        "TRADE_CREATED",
                        Instant.EPOCH,
                        "tester",
                        null,
                        "{invalid-json")));

        assertThatThrownBy(() -> controller.events(tradeRef))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid after-state JSON for eventId=" + eventId);
    }

    private AuditLogEntry entry(UUID eventId, String tradeRef,
                                String eventType, long timestampOffset) {
        String before = "TRADE_UPDATED".equals(eventType)
                ? "{\"tradeRef\":\"" + tradeRef
                        + "\",\"details\":{\"status\":\"PENDING\"}}"
                : null;
        String status = "TRADE_UPDATED".equals(eventType) ? "MATCHED" : "PENDING";
        return new AuditLogEntry(
                eventId.toString(),
                tradeRef,
                eventType,
                Instant.EPOCH.plusSeconds(timestampOffset),
                "tester",
                before,
                "{\"tradeRef\":\"" + tradeRef
                        + "\",\"details\":{\"status\":\"" + status + "\"}}");
    }
}
