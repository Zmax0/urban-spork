package com.urbanspork.common.codec;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SupportedCipher {

    @JsonProperty("aes-128-gcm")
    aes_128_gcm,
    @JsonProperty("aes-192-gcm")
    aes_192_gcm,
    @JsonProperty("aes-256-gcm")
    aes_256_gcm,
    @JsonProperty("chacha20-poly1305")
    chacha20_poly1305

}
