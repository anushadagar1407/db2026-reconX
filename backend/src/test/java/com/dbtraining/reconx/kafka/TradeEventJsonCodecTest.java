package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeEventJsonCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TradeEventJsonCodec codec = new TradeEventJsonCodec(objectMapper);

    @Test
    void roundTripsNestedSnapshotsAndActorWithoutDoubleEncoding() throws Exception {
        JsonNode before = objectMapper.readTree("""
                {"details":{"status":"PENDING"},"allocations":[{"quantity":25.5000}]}
                """);
        JsonNode after = objectMapper.readTree("""
                {"details":{"status":"MATCHED"},"allocations":[{"quantity":30.7500}]}
                """);
        TradeEvent event = TradeEvent.updated(
                "TRD-CODEC", "operator@db.com", before, after);

        String encoded = codec.encode(event);
        JsonNode encodedTree = objectMapper.readTree(encoded);
        TradeEvent restored = codec.decode(encoded);

        assertThat(encodedTree.path("before").isObject()).isTrue();
        assertThat(encodedTree.path("after").path("allocations").isArray()).isTrue();
        assertThat(restored).isEqualTo(event);
        assertThat(restored.actor()).isEqualTo("operator@db.com");
        assertThat(restored.before().path("details").path("status").asText())
                .isEqualTo("PENDING");
        assertThat(restored.after().path("allocations").get(0).path("quantity").decimalValue())
                .isEqualByComparingTo("30.7500");
    }

    @Test
    void rejectsInvalidStoredPayloadWithControlledFailure() {
        assertThatThrownBy(() -> codec.decode("{invalid-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not deserialize stored DLQ trade event");
    }
}
