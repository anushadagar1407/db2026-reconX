package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventJsonCodec;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DlqAdminControllerTest {

    private final DlqMessageRepository repository = mock(DlqMessageRepository.class);
    private final TradeEventProducer producer = mock(TradeEventProducer.class);
    private final TradeEventJsonCodec codec = new TradeEventJsonCodec(
            new ObjectMapper().findAndRegisterModules());
    private final DlqAdminController controller =
            new DlqAdminController(repository, producer, codec);

    @Test
    void dryRunReturnsPreviewWithoutPublishingOrDeleting() {
        TradeEvent event = TradeEvent.created("TRD-PREVIEW");
        DlqMessage message = messageFor(event);
        when(repository.findByEventId(event.eventId().toString()))
                .thenReturn(Optional.of(message));

        ResponseEntity<Map<String, Object>> response =
                controller.replay(event.eventId(), true);

        assertThat(response.getBody())
                .containsEntry("dryRun", true)
                .containsEntry("eventId", event.eventId())
                .containsEntry("wouldReplayTo", "trade-events")
                .containsEntry("tradeRef", "TRD-PREVIEW")
                .containsEntry("payload", event);
        verify(producer, never()).publish(event);
        verify(repository, never()).delete(message);
    }

    @Test
    void replayPublishesEventAndDeletesStoredMessage() {
        TradeEvent event = TradeEvent.created("TRD-REPLAY");
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
        verify(producer).publish(event);
        verify(repository).delete(message);
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
