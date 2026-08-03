package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**

 */
@Component
public class AlertProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertProducer.class);
    private static final String TOPIC = "system-alerts";

    private final KafkaTemplate<String, TradeEvent> template;

    public AlertProducer(KafkaTemplate<String, TradeEvent> template) {
        this.template = template;
    }

    public void publish(TradeEvent event) {
        template.send(TOPIC, "Issue creating event", event);
    }
}