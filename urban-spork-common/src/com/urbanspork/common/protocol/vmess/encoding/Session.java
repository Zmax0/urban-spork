package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.crypto.GeneralDigests;

import java.util.Arrays;

public abstract class Session {

    final byte[] requestBodyIV;
    final byte[] requestBodyKey;
    final byte[] responseBodyIV;
    final byte[] responseBodyKey;
    final byte responseHeader;

    protected Session(byte[] requestBodyIV, byte[] requestBodyKey, byte responseHeader) {
        this.requestBodyIV = requestBodyIV;
        this.requestBodyKey = requestBodyKey;
        this.responseHeader = responseHeader;
        this.responseBodyIV = Arrays.copyOf(GeneralDigests.sha256.get(requestBodyIV), 16);
        this.responseBodyKey = Arrays.copyOf(GeneralDigests.sha256.get(requestBodyKey), 16);
    }

    public byte[] getRequestBodyIV() {
        return requestBodyIV;
    }

    public byte[] getRequestBodyKey() {
        return requestBodyKey;
    }

    public byte[] getResponseBodyIV() {
        return responseBodyIV;
    }

    public byte[] getResponseBodyKey() {
        return responseBodyKey;
    }

    public byte getResponseHeader() {
        return responseHeader;
    }
}
