package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import com.dbtraining.reconx.dto.TradeEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/** Listener factories for Kafka payload types other than TradeEvent. */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnMissingBean(AlertSink.class)
    public AlertSink alertSink() {
        return new NoopAlertSink();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent>
            tradeEventListenerContainerFactory(
                    KafkaProperties kafkaProperties,
                    DefaultErrorHandler errorHandler,
                    MeterRegistry meterRegistry) {
        JsonDeserializer<TradeEvent> jsonDeserializer =
                new JsonDeserializer<>(TradeEvent.class, false);
        jsonDeserializer.addTrustedPackages("com.dbtraining.reconx.dto");

        DefaultKafkaConsumerFactory<String, TradeEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        typedConsumerProperties(kafkaProperties),
                        new StringDeserializer(),
                        new ErrorHandlingDeserializer<>(jsonDeserializer));
        consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));

        ConcurrentKafkaListenerContainerFactory<String, TradeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SystemAlert>
            systemAlertListenerContainerFactory(
                    KafkaProperties kafkaProperties,
                    MeterRegistry meterRegistry) {
        JsonDeserializer<SystemAlert> jsonDeserializer =
                new JsonDeserializer<>(SystemAlert.class, false);
        jsonDeserializer.addTrustedPackages("com.dbtraining.reconx.dto");

        DefaultKafkaConsumerFactory<String, SystemAlert> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        typedConsumerProperties(kafkaProperties),
                        new StringDeserializer(),
                        new ErrorHandlingDeserializer<>(jsonDeserializer));
        consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));

        ConcurrentKafkaListenerContainerFactory<String, SystemAlert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    private Map<String, Object> typedConsumerProperties(KafkaProperties kafkaProperties) {
        Map<String, Object> properties =
                new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        properties.keySet().removeIf(key -> key.startsWith("spring.json."));
        return properties;
    }
}
