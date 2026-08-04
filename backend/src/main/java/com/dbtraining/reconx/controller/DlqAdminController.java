package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventJsonCodec;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Administrative endpoint for one-at-a-time DLQ replay. */
@RestController
@RequestMapping("/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
public class DlqAdminController {

    private final DlqMessageRepository repository;
    private final TradeEventProducer producer;
    private final TradeEventJsonCodec codec;

    public DlqAdminController(DlqMessageRepository repository,
                              TradeEventProducer producer,
                              TradeEventJsonCodec codec) {
        this.repository = repository;
        this.producer = producer;
        this.codec = codec;
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        DlqMessage message = repository.findByEventId(eventId.toString())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "No DLQ message for eventId=" + eventId));
        TradeEvent event = codec.decode(message.getPayload());

        if (dryRun) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "eventId", eventId,
                    "wouldReplayTo", message.getOriginalTopic(),
                    "tradeRef", message.getTradeRef(),
                    "payload", event));
        }

        producer.publish(event);
        repository.delete(message);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", eventId,
                "topic", message.getOriginalTopic(),
                "tradeRef", message.getTradeRef()));
    }
}
