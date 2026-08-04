package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditControllerTest {

    @Test
    void returnsKafkaTradeEventsInRepositoryOrder() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository);
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
        verify(repository).findByTradeRefOrderByEventTimestampAsc(tradeRef);
    }

    private AuditLogEntry entry(UUID eventId, String tradeRef,
                                String eventType, long timestampOffset) {
        return new AuditLogEntry(
                eventId.toString(),
                tradeRef,
                eventType,
                Instant.EPOCH.plusSeconds(timestampOffset),
                "tester",
                null,
                "{\"tradeRef\":\"" + tradeRef + "\"}");
    }
}
