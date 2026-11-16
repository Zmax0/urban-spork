package com.urbanspork.test.interop;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

record InteropPeerImages(Image shadowsocksClient, Image shadowsocksServer, Image vmess, Image trojan) {
    record Image(String name, String tag, String digest) {
        String reference() {
            return name + "@" + digest;
        }
    }

    static InteropPeerImages load() {
        try (var input = InteropPeerImages.class.getResourceAsStream("/interop-peers.json")) {
            if (input == null) {
                throw new IOException("Missing test resource: interop-peers.json");
            }
            return JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                .build().readValue(input, InteropPeerImages.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
