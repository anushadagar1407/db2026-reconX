package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeAggregatorTest {

    private static final String TRADE_REF = "TRD-137";

    private final AuditLogRepository auditRepository = mock(AuditLogRepository.class);
    private final TradeAggregator aggregator =
            new TradeAggregator(auditRepository, new ObjectMapper());

    @Test
    void returnsEmptyWhenTradeHasNoEvents() {
        when(auditRepository.findByTradeRefOrderByEventTimestampAsc(TRADE_REF))
                .thenReturn(List.of());

        Optional<JsonNode> rebuilt = aggregator.rebuild(TRADE_REF);

        assertThat(rebuilt).isEmpty();
        verify(auditRepository).findByTradeRefOrderByEventTimestampAsc(TRADE_REF);
    }

    @Test
    void returnsLatestStateAfterCreatedAndUpdatedEvents() {
        when(auditRepository.findByTradeRefOrderByEventTimestampAsc(TRADE_REF))
                .thenReturn(List.of(
                        event("event-created", "TRADE_CREATED",
                                "{\"details\":{\"status\":\"PENDING\"}}", 1),
                        event("event-updated", "TRADE_UPDATED",
                                "{\"details\":{\"status\":\"MATCHED\"}}", 2)));

        Optional<JsonNode> rebuilt = aggregator.rebuild(TRADE_REF);

        assertThat(rebuilt).isPresent();
        assertThat(rebuilt.orElseThrow().isObject()).isTrue();
        assertThat(rebuilt.orElseThrow().path("details").path("status").asText())
                .isEqualTo("MATCHED");
    }

    @Test
    void returnsEmptyWhenLastTradeEventIsCancelled() {
        when(auditRepository.findByTradeRefOrderByEventTimestampAsc(TRADE_REF))
                .thenReturn(List.of(
                        event("event-created", "TRADE_CREATED", "{\"status\":\"PENDING\"}", 1),
                        event("event-updated", "TRADE_UPDATED", "{\"status\":\"MATCHED\"}", 2),
                        event("event-cancelled", "TRADE_CANCELLED", null, 3)));

        Optional<JsonNode> rebuilt = aggregator.rebuild(TRADE_REF);

        assertThat(rebuilt).isEmpty();
    }

    @Test
    void ignoresLegacyAuditOperationsWhenFoldingKafkaEvents() {
        when(auditRepository.findByTradeRefOrderByEventTimestampAsc(TRADE_REF))
                .thenReturn(List.of(
                        event("event-created", "TRADE_CREATED", "{\"status\":\"PENDING\"}", 1),
                        event("legacy-event", "READ", null, 2)));

        Optional<JsonNode> rebuilt = aggregator.rebuild(TRADE_REF);

        assertThat(rebuilt).isPresent();
        assertThat(rebuilt.orElseThrow().path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void rejectsInvalidStoredSnapshotWithEventContext() {
        when(auditRepository.findByTradeRefOrderByEventTimestampAsc(TRADE_REF))
                .thenReturn(List.of(
                        event("event-invalid", "TRADE_CREATED", "{invalid-json", 1)));

        assertThatThrownBy(() -> aggregator.rebuild(TRADE_REF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid after-state JSON for eventId=event-invalid");
    }

    private AuditLogEntry event(String eventId, String eventType, String afterState,
                                long timestampOffset) {
        return new AuditLogEntry(
                eventId,
                TRADE_REF,
                eventType,
                Instant.EPOCH.plusSeconds(timestampOffset),
                "test",
                null,
                afterState);
    }
}
