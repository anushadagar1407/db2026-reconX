package com.dbtraining.reconx.kafka;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TradeEventProducerTest {

    private static final String TOPIC = "trade-events";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesWithTradeReferenceAsKeyWithoutWaitingForCompletion() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TradeEvent> template = mock(KafkaTemplate.class);
        @SuppressWarnings("unchecked")
        SendResult<String, TradeEvent> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        CompletableFuture<SendResult<String, TradeEvent>> send = new CompletableFuture<>();
        TradeEvent event = TradeEvent.created(
                "TRD-KEYED", objectMapper.createObjectNode().put("status", "PENDING"));
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(template.send(TOPIC, event.tradeRef(), event)).thenReturn(send);

        new TradeEventProducer(template).publish(event);

        verify(template).send(TOPIC, "TRD-KEYED", event);
        verifyNoInteractions(metadata);

        send.complete(result);
        verify(metadata).partition();
        verify(metadata).offset();
    }

    @Test
    void asynchronousFailureIsLoggedWithoutEscapingToTheCaller() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TradeEvent> template = mock(KafkaTemplate.class);
        TradeEvent event = TradeEvent.cancelled(
                "TRD-FAILED", objectMapper.createObjectNode().put("status", "PENDING"));
        CompletableFuture<SendResult<String, TradeEvent>> send = new CompletableFuture<>();
        when(template.send(TOPIC, event.tradeRef(), event)).thenReturn(send);
        Logger logger = (Logger) LoggerFactory.getLogger(TradeEventProducer.class);
        ListAppender<ILoggingEvent> logs = captureLogs(logger);

        try {
            new TradeEventProducer(template).publish(event);
            assertThatCode(() -> send.completeExceptionally(new RuntimeException("broker unavailable")))
                    .doesNotThrowAnyException();

            assertThat(logs.list).anySatisfy(log -> {
                assertThat(log.getLevel()).isEqualTo(Level.ERROR);
                assertThat(log.getFormattedMessage())
                        .contains("Failed to publish TradeEvent")
                        .contains(event.eventId().toString())
                        .contains("TRD-FAILED");
                assertThat(log.getThrowableProxy().getMessage()).isEqualTo("broker unavailable");
            });
        } finally {
            logger.detachAppender(logs);
        }
    }

    @Test
    void synchronousSendFailureIsLoggedAndCannotEscapeAfterCommit() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TradeEvent> template = mock(KafkaTemplate.class);
        TradeEvent event = TradeEvent.created(
                "TRD-SYNC-FAILED", objectMapper.createObjectNode().put("status", "PENDING"));
        when(template.send(TOPIC, event.tradeRef(), event))
                .thenThrow(new RuntimeException("metadata timeout"));
        Logger logger = (Logger) LoggerFactory.getLogger(TradeEventProducer.class);
        ListAppender<ILoggingEvent> logs = captureLogs(logger);

        try {
            assertThatCode(() -> new TradeEventProducer(template).publish(event))
                    .doesNotThrowAnyException();

            assertThat(logs.list).anySatisfy(log -> assertThat(log.getFormattedMessage())
                    .contains("Failed to initiate TradeEvent publish")
                    .contains("TRD-SYNC-FAILED"));
        } finally {
            logger.detachAppender(logs);
        }
    }

    @Test
    void successMetadataFailureIsLoggedWithoutEscaping() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TradeEvent> template = mock(KafkaTemplate.class);
        @SuppressWarnings("unchecked")
        SendResult<String, TradeEvent> result = mock(SendResult.class);
        TradeEvent event = TradeEvent.created(
                "TRD-METADATA-FAILED", objectMapper.createObjectNode().put("status", "PENDING"));
        when(result.getRecordMetadata()).thenThrow(new RuntimeException("metadata unavailable"));
        when(template.send(TOPIC, event.tradeRef(), event))
                .thenReturn(CompletableFuture.completedFuture(result));
        Logger logger = (Logger) LoggerFactory.getLogger(TradeEventProducer.class);
        ListAppender<ILoggingEvent> logs = captureLogs(logger);

        try {
            assertThatCode(() -> new TradeEventProducer(template).publish(event))
                    .doesNotThrowAnyException();

            assertThat(logs.list).anySatisfy(log -> assertThat(log.getFormattedMessage())
                    .contains("Failed to read publish metadata")
                    .contains("TRD-METADATA-FAILED"));
        } finally {
            logger.detachAppender(logs);
        }
    }

    @Test
    void applicationEventsSendOnlyAfterCommitAndAreDiscardedOnRollback() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, TradeEvent> template = mock(KafkaTemplate.class);
        @SuppressWarnings("unchecked")
        SendResult<String, TradeEvent> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(template.send(anyString(), anyString(), any(TradeEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionTestConfiguration.class);
            context.registerBean(TradeEventProducer.class, () -> new TradeEventProducer(template));
            context.refresh();
            TransactionTemplate transactions = new TransactionTemplate(
                    context.getBean(PlatformTransactionManager.class));
            TradeEvent committed = TradeEvent.created(
                    "TRD-COMMIT", objectMapper.createObjectNode().put("status", "PENDING"));

            transactions.executeWithoutResult(status -> {
                context.publishEvent(committed);
                verifyNoInteractions(template);
            });

            verify(template).send(TOPIC, "TRD-COMMIT", committed);
            clearInvocations(template);

            TradeEvent rolledBack = TradeEvent.created(
                    "TRD-ROLLBACK", objectMapper.createObjectNode().put("status", "PENDING"));
            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
                context.publishEvent(rolledBack);
                throw new IllegalStateException("force rollback");
            })).isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(template);

            context.publishEvent(TradeEvent.created(
                    "TRD-NO-TRANSACTION",
                    objectMapper.createObjectNode().put("status", "PENDING")));
            verifyNoInteractions(template);
        }
    }

    private static ListAppender<ILoggingEvent> captureLogs(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionTestConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
