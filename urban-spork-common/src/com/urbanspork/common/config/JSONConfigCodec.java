package com.urbanspork.common.config;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JSONConfigCodec implements ConfigCodec {

    private final ObjectMapper mapper;

    public JSONConfigCodec() {
        this.mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build();
    }

    @Override
    public String encode(ClientConfig config) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
    }

    @Override
    public ClientConfig decode(String config) {
        return mapper.readValue(config, ClientConfig.class);
    }
}