package com.dbtraining.reconx.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads both JSON objects and legacy JSON-string rows from the H2 dev slice.
 */
public class JsonObjectMapper extends ObjectMapper {

    @Override
    public <T> T readValue(String content, JavaType valueType) throws JsonProcessingException {
        JsonNode node = super.readTree(content);
        return super.readValue(node.isTextual() ? node.textValue() : content, valueType);
    }
}
