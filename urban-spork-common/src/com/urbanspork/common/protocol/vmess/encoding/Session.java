package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.util.ByteString;

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
        this.responseBodyIV = Arrays.copyOf(Digests.sha256.hash(requestBodyIV), 16);
        this.responseBodyKey = Arrays.copyOf(Digests.sha256.hash(requestBodyKey), 16);
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

    @Override
    public String toString() {
        return String.format("RK:%s, RI:%s, SK:%s, SI:%s, SH:%d",
            ByteString.valueOf(requestBodyKey),
            ByteString.valueOf(requestBodyIV),
            ByteString.valueOf(responseBodyKey),
            ByteString.valueOf(responseBodyIV),
            Byte.toUnsignedInt(responseHeader));
    }
}
