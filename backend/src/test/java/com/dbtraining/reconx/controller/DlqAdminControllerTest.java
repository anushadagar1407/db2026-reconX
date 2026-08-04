package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventJsonCodec;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DlqAdminControllerTest {

    private final DlqMessageRepository repository = mock(DlqMessageRepository.class);
    private final TradeEventProducer producer = mock(TradeEventProducer.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TradeEventJsonCodec codec = new TradeEventJsonCodec(objectMapper);
    private final DlqAdminController controller =
            new DlqAdminController(repository, producer, codec);

    @Test
    void dryRunReturnsPreviewWithoutPublishingOrDeleting() {
        TradeEvent event = createdEvent("TRD-PREVIEW");
        DlqMessage message = messageFor(event);
        when(repository.findByEventId(event.eventId().toString()))
                .thenReturn(Optional.of(message));

        ResponseEntity<Map<String, Object>> response =
                controller.replay(event.eventId(), true);

        assertThat(response.getBody())
                .containsEntry("dryRun", true)
                .containsEntry("eventId", event.eventId())
                .containsEntry("wouldReplayTo", "trade-events")
                .containsEntry("tradeRef", "TRD-PREVIEW");
        TradeEvent preview = (TradeEvent) response.getBody().get("payload");
        assertThat(preview.eventId()).isEqualTo(event.eventId());
        assertThat(preview.actor()).isEqualTo("dlq-admin@db.com");
        assertThat(preview.after()).isEqualTo(event.after());
        assertThat(preview.after().path("details").path("status").asText())
                .isEqualTo("PENDING");
        verify(producer, never()).publish(any(TradeEvent.class));
        verify(repository, never()).delete(message);
    }

    @Test
    void replayPublishesEventAndDeletesStoredMessage() {
        TradeEvent event = createdEvent("TRD-REPLAY");
        DlqMessage message = messageFor(event);
        when(repository.findByEventId(event.eventId().toString()))
                .thenReturn(Optional.of(message));

        ResponseEntity<Map<String, Object>> response =
                controller.replay(event.eventId(), false);

        assertThat(response.getBody())
                .containsEntry("replayed", true)
                .containsEntry("eventId", event.eventId())
                .containsEntry("topic", "trade-events")
                .containsEntry("tradeRef", event.tradeRef());
        ArgumentCaptor<TradeEvent> replayed = ArgumentCaptor.forClass(TradeEvent.class);
        verify(producer).publish(replayed.capture());
        assertThat(replayed.getValue().eventId()).isEqualTo(event.eventId());
        assertThat(replayed.getValue().actor()).isEqualTo("dlq-admin@db.com");
        assertThat(replayed.getValue().after()).isEqualTo(event.after());
        assertThat(replayed.getValue().after().path("details").path("status").asText())
                .isEqualTo("PENDING");
        verify(repository).delete(message);
    }

    private TradeEvent createdEvent(String tradeRef) {
        return TradeEvent.created(
                tradeRef,
                "dlq-admin@db.com",
                objectMapper.createObjectNode()
                        .put("tradeRef", tradeRef)
                        .set("details", objectMapper.createObjectNode()
                                .put("status", "PENDING")));
    }

    private DlqMessage messageFor(TradeEvent event) {
        return new DlqMessage(
                event.eventId().toString(),
                event.tradeRef(),
                "trade-events",
                1,
                12L,
                codec.encode(event),
                "boom",
                Instant.now());
    }
}
