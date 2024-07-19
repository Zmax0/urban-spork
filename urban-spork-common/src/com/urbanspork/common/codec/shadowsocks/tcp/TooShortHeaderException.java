package com.urbanspork.common.codec.shadowsocks.tcp;

import io.netty.handler.codec.DecoderException;

class TooShortHeaderException extends DecoderException {
    TooShortHeaderException(String message) {
        super(message);
    }
}
