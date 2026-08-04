package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/** Listener factories for Kafka payload types other than TradeEvent. */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnMissingBean(AlertSink.class)
    public AlertSink alertSink() {
        return new NoopAlertSink();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SystemAlert>
            systemAlertListenerContainerFactory(KafkaProperties kafkaProperties) {
        JsonDeserializer<SystemAlert> jsonDeserializer =
                new JsonDeserializer<>(SystemAlert.class, false);
        jsonDeserializer.addTrustedPackages("com.dbtraining.reconx.dto");

        DefaultKafkaConsumerFactory<String, SystemAlert> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        kafkaProperties.buildConsumerProperties(null),
                        new StringDeserializer(),
                        new ErrorHandlingDeserializer<>(jsonDeserializer));

        ConcurrentKafkaListenerContainerFactory<String, SystemAlert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
