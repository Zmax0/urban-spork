package com.urbanspork.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

class JSONConfigCodec implements ConfigCodec {

    private final ObjectMapper mapper;

    public JSONConfigCodec() {
        this.mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build();
    }

    @Override
    public String encode(ClientConfig config) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
    }

    @Override
    public ClientConfig decode(String config) throws JsonProcessingException {
        return mapper.readValue(config, ClientConfig.class);
    }
}