package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Persists failed trade events so operators can inspect and replay them. */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repository;
    private final TradeEventJsonCodec codec;

    public DlqConsumer(DlqMessageRepository repository, TradeEventJsonCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    @KafkaListener(
            topics = "trade-events-dlq",
            groupId = "dlq-monitor",
            containerFactory = "tradeEventListenerContainerFactory")
    @Transactional
    public void onDlqMessage(
            ConsumerRecord<String, TradeEvent> record,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false)
            String exceptionMessage,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false)
            String dltExceptionMessage,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false)
            String originalTopic,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_PARTITION, required = false)
            Integer originalPartition,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false)
            Long originalOffset) {
        TradeEvent event = record.value();
        String reason = firstNonBlank(dltExceptionMessage, exceptionMessage,
                "Unknown listener failure");
        String sourceTopic = firstNonBlank(originalTopic,
                record.topic().replaceFirst("-dlq$", ""));
        int sourcePartition = originalPartition != null ? originalPartition : record.partition();
        long sourceOffset = originalOffset != null ? originalOffset : record.offset();

        log.error(
                "DLQ eventId={} tradeRef={} topic={} partition={} offset={} reason={}",
                event.eventId(), event.tradeRef(), sourceTopic,
                sourcePartition, sourceOffset, reason);

        if (repository.existsByEventId(event.eventId().toString())) {
            log.warn("DLQ event already persisted eventId={}", event.eventId());
            return;
        }

        repository.save(new DlqMessage(
                event.eventId().toString(),
                event.tradeRef(),
                sourceTopic,
                sourcePartition,
                sourceOffset,
                codec.encode(event),
                reason,
                Instant.now()));
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
