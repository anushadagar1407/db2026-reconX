package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Converts TradeEvent payloads to and from the durable DLQ JSON representation. */
@Component
public class TradeEventJsonCodec {

    private final ObjectMapper objectMapper;

    public TradeEventJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(TradeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize DLQ trade event", exception);
        }
    }

    public TradeEvent decode(String payload) {
        try {
            return objectMapper.readValue(payload, TradeEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize stored DLQ trade event", exception);
        }
    }
}
