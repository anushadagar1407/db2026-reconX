package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DlqConsumerTest {

    @Test
    void persistsFailedEventWithOriginalKafkaCoordinates() {
        DlqMessageRepository repository = mock(DlqMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        TradeEventJsonCodec codec = new TradeEventJsonCodec(objectMapper);
        DlqConsumer consumer = new DlqConsumer(repository, codec);
        JsonNode after = objectMapper.createObjectNode()
                .put("tradeRef", "TRD-DLQ-1")
                .set("details", objectMapper.createObjectNode().put("status", "PENDING"));
        TradeEvent event = TradeEvent.created("TRD-DLQ-1", "consumer@db.com", after);
        ConsumerRecord<String, TradeEvent> record =
                new ConsumerRecord<>("trade-events-dlq", 2, 9L, event.tradeRef(), event);
        when(repository.existsByEventId(event.eventId().toString())).thenReturn(false);

        consumer.onDlqMessage(
                record,
                null,
                "reconciliation failed",
                "trade-events",
                2,
                7L);

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(repository).save(captor.capture());
        DlqMessage saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.eventId().toString());
        assertThat(saved.getTradeRef()).isEqualTo("TRD-DLQ-1");
        assertThat(saved.getOriginalTopic()).isEqualTo("trade-events");
        assertThat(saved.getOriginalPartition()).isEqualTo(2);
        assertThat(saved.getOriginalOffset()).isEqualTo(7L);
        assertThat(saved.getReason()).isEqualTo("reconciliation failed");
        TradeEvent restored = codec.decode(saved.getPayload());
        assertThat(restored.eventId()).isEqualTo(event.eventId());
        assertThat(restored.tradeRef()).isEqualTo(event.tradeRef());
        assertThat(restored.eventType()).isEqualTo(event.eventType());
        assertThat(restored.timestamp()).isEqualTo(event.timestamp());
        assertThat(restored.actor()).isEqualTo("consumer@db.com");
        assertThat(restored.after()).isEqualTo(event.after());
        assertThat(restored.after().path("details").path("status").asText())
                .isEqualTo("PENDING");
        assertThat(saved.getFirstSeen()).isNotNull();
    }
}
