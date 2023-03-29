package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.protocol.vmess.VMessProtocol;

import java.util.Arrays;

public class ClientSession {

    final byte[] requestBodyIV;
    final byte[] requestBodyKey;
    final byte[] responseBodyIV;
    final byte[] responseBodyKey;
    final byte responseHeader;

    public ClientSession() {
        this(CipherCodec.randomBytes(33));
    }

    public ClientSession(byte[] bytes) {
        this.responseHeader = bytes[32];
        this.requestBodyIV = Arrays.copyOf(bytes, 16);
        this.requestBodyKey = Arrays.copyOfRange(bytes, 16, 32);
        responseBodyIV = Arrays.copyOf(VMessProtocol.sha256(requestBodyIV), 16);
        responseBodyKey = Arrays.copyOf(VMessProtocol.sha256(requestBodyKey), 16);
    }
}
