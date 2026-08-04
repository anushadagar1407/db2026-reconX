package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * TICKET-ADV133 — AlertConsumer
 *
 * WHAT:    Subscribes to `system-alerts`, logs each structured alert and
 *          forwards it to a pluggable notification sink.
 * HOW:     @KafkaListener on the `system-alerts` topic, groupId
 *          `alert-service`.
 * WHY:     Decouples alert producers (any service) from alert sinks
 *          (notification channels).
 * OBSERVE: Publish a SystemAlert to `system-alerts` -> a WARN line appears
 *          in the app log and the configured AlertSink is invoked.
 * ============================================================================
 */
@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final AlertSink sink;

    public AlertConsumer(AlertSink sink) {
        this.sink = sink;
    }

    @KafkaListener(
            topics = "system-alerts",
            groupId = "alert-service",
            containerFactory = "systemAlertListenerContainerFactory")
    public void onAlert(SystemAlert alert) {
        log.warn("ALERT severity={} code={} message={}",
                alert.severity(), alert.code(), alert.message());
        sink.notify(alert);
    }
}
