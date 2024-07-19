package com.urbanspork.common.codec.shadowsocks.tcp;

import io.netty.handler.codec.DecoderException;

class RepeatedNonceException extends DecoderException {
    RepeatedNonceException(String message) {
        super(message);
    }
}
