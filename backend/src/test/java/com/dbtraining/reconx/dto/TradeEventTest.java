package com.dbtraining.reconx.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void factoriesCreateFreshMetadataAndLifecycleSnapshots() {
        JsonNode before = objectMapper.createObjectNode().put("status", "PENDING");
        JsonNode after = objectMapper.createObjectNode().put("status", "MATCHED");
        Instant startedAt = Instant.now();

        TradeEvent created = TradeEvent.created("TRD-1", "creator", after);
        TradeEvent updated = TradeEvent.updated("TRD-1", "editor", before, after);
        TradeEvent cancelled = TradeEvent.cancelled("TRD-1", "admin", before);

        assertThat(List.of(created.eventId(), updated.eventId(), cancelled.eventId()))
                .doesNotHaveDuplicates();
        assertThat(created.timestamp()).isAfterOrEqualTo(startedAt);
        assertThat(updated.timestamp()).isAfterOrEqualTo(startedAt);
        assertThat(cancelled.timestamp()).isAfterOrEqualTo(startedAt);

        assertThat(created.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CREATED);
        assertThat(created.actor()).isEqualTo("creator");
        assertThat(created.before()).isNull();
        assertThat(created.after()).isEqualTo(after);

        assertThat(updated.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(updated.actor()).isEqualTo("editor");
        assertThat(updated.before()).isEqualTo(before);
        assertThat(updated.after()).isEqualTo(after);

        assertThat(cancelled.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CANCELLED);
        assertThat(cancelled.actor()).isEqualTo("admin");
        assertThat(cancelled.before()).isEqualTo(before);
        assertThat(cancelled.after()).isNull();
    }

    @Test
    void jacksonRoundTripPreservesEveryWireFieldAndDecimalValues() throws Exception {
        JsonNode before = objectMapper.readTree("""
                {"quantity":100.0000,"price":245.5000,"status":"PENDING"}
                """);
        JsonNode after = objectMapper.readTree("""
                {"quantity":125.0000,"price":250.2500,"status":"MATCHED"}
                """);
        TradeEvent event = TradeEvent.updated("TRD-ROUNDTRIP", "trader@db.com", before, after);

        TradeEvent restored = objectMapper.readValue(
                objectMapper.writeValueAsBytes(event), TradeEvent.class);

        assertThat(restored).isEqualTo(event);
        assertThat(restored.before().path("quantity").decimalValue())
                .isEqualByComparingTo("100.0000");
        assertThat(restored.after().path("price").decimalValue())
                .isEqualByComparingTo("250.2500");
    }
}
