package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditEventConsumer consumer = new AuditEventConsumer(repository);

    @Test
    void mapsEveryEventFieldToTheExistingAuditSchema() {
        JsonNode before = objectMapper.createObjectNode()
                .put("quantity", "100.0000")
                .put("status", "PENDING");
        JsonNode after = objectMapper.createObjectNode()
                .put("quantity", "125.0000")
                .put("status", "MATCHED");
        TradeEvent event = TradeEvent.updated("TRD-AUDIT", "trader@db.com", before, after);

        consumer.onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> row = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(row.capture());
        assertThat(row.getValue().getEventId()).isEqualTo(event.eventId().toString());
        assertThat(row.getValue().getTradeRef()).isEqualTo("TRD-AUDIT");
        assertThat(row.getValue().getEventType()).isEqualTo("TRADE_UPDATED");
        assertThat(row.getValue().getEventTimestamp()).isEqualTo(event.timestamp());
        assertThat(row.getValue().getActor()).isEqualTo("trader@db.com");
        assertThat(row.getValue().getBeforeState()).isEqualTo(before.toString());
        assertThat(row.getValue().getAfterState()).isEqualTo(after.toString());
    }

    @Test
    void duplicateRedeliveryIsIdempotentlySuccessful() {
        TradeEvent event = TradeEvent.created(
                "TRD-DUPLICATE", objectMapper.createObjectNode().put("status", "PENDING"));
        when(repository.existsByEventId(event.eventId().toString())).thenReturn(false, true);

        assertThatCode(() -> {
            consumer.onTradeEvent(event);
            consumer.onTradeEvent(event);
        }).doesNotThrowAnyException();

        verify(repository, times(2)).existsByEventId(event.eventId().toString());
        verify(repository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void existingDuplicateDoesNotAttemptAnotherInsert() {
        TradeEvent event = TradeEvent.cancelled(
                "TRD-DUPLICATE", objectMapper.createObjectNode().put("status", "MATCHED"));
        when(repository.existsByEventId(event.eventId().toString())).thenReturn(true);

        consumer.onTradeEvent(event);

        verify(repository, never()).save(any(AuditLogEntry.class));
    }

    @Test
    void javaAndJacksonNullNodesMapToDatabaseNulls() {
        TradeEvent event = new TradeEvent(
                UUID.randomUUID(),
                "TRD-CREATE",
                TradeEvent.EventType.TRADE_CREATED,
                Instant.parse("2026-08-04T12:00:00Z"),
                "creator",
                NullNode.getInstance(),
                null);

        consumer.onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> row = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(row.capture());
        assertThat(row.getValue().getBeforeState()).isNull();
        assertThat(row.getValue().getAfterState()).isNull();
    }

    @Test
    void listenerIsTransactionalAndUsesAuditGroupAndSharedErrorHandlerFactory() throws Exception {
        Method listener = AuditEventConsumer.class.getMethod("onTradeEvent", TradeEvent.class);
        KafkaListener kafka = listener.getAnnotation(KafkaListener.class);

        assertThat(kafka.topics()).containsExactly("trade-events");
        assertThat(kafka.groupId()).isEqualTo("audit-service");
        assertThat(kafka.containerFactory()).isEqualTo("tradeEventListenerContainerFactory");
        assertThat(listener.getAnnotation(Transactional.class)).isNotNull();
    }
}
