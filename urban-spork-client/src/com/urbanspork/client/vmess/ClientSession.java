package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.crypto.GeneralDigests;

import java.util.Arrays;

class ClientSession {

    final byte[] requestBodyIV;
    final byte[] requestBodyKey;
    final byte[] responseBodyIV;
    final byte[] responseBodyKey;
    final byte responseHeader;

    ClientSession() {
        this(CipherCodec.randomBytes(33));
    }

    ClientSession(byte[] bytes) {
        this.responseHeader = bytes[32];
        this.requestBodyIV = Arrays.copyOf(bytes, 16);
        this.requestBodyKey = Arrays.copyOfRange(bytes, 16, 32);
        responseBodyIV = Arrays.copyOf(GeneralDigests.sha256.get(requestBodyIV), 16);
        responseBodyKey = Arrays.copyOf(GeneralDigests.sha256.get(requestBodyKey), 16);
    }
}
