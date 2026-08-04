package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ============================================================================
 * TICKET-ADV129 — TradeEventProducer
 *
 * WHAT:    Publishes TradeEvent messages to the `trade-events` Kafka topic.
 * HOW:     KafkaTemplate<String, TradeEvent>. Key = tradeRef so that all
 *          events for the same trade hash to the same partition and
 *          preserve ordering.
 * WHY:     Out-of-order events for the same trade would make event sourcing
 *          impossible (you'd "apply" CREATE after UPDATE).
 * OBSERVE: Kafdrop -> `trade-events` shows one message per published event,
 *          partitioned by tradeRef.
 * ============================================================================
 */
@Component
public class TradeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);
    private static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, TradeEvent> template;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> template) {
        this.template = template;
    }

    public void publish(TradeEvent event) {
        log.debug("Publishing TradeEvent eventId={} ref={} type={}",
                event.eventId(), event.tradeRef(), event.eventType());
        try {
            template.send(TOPIC, event.tradeRef(), event)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Failed to publish TradeEvent eventId={} ref={}",
                                    event.eventId(), event.tradeRef(), failure);
                            return;
                        }
                        try {
                            RecordMetadata metadata = result.getRecordMetadata();
                            log.debug(
                                    "Published TradeEvent eventId={} ref={} partition={} offset={}",
                                    event.eventId(),
                                    event.tradeRef(),
                                    metadata.partition(),
                                    metadata.offset());
                        } catch (RuntimeException metadataFailure) {
                            log.error(
                                    "Failed to read publish metadata for TradeEvent eventId={} ref={}",
                                    event.eventId(), event.tradeRef(), metadataFailure);
                        }
                    });
        } catch (RuntimeException failure) {
            log.error("Failed to initiate TradeEvent publish eventId={} ref={}",
                    event.eventId(), event.tradeRef(), failure);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onTradeEventCommitted(TradeEvent event) {
        publish(event);
    }
}
