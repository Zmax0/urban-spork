package com.urbanspork.common.protocol.vmess.encoding;

public class ServerSession extends Session {

    public ServerSession(ClientSession session) {
        super(session.requestBodyIV, session.requestBodyKey, session.responseHeader);
    }

    public ServerSession(byte[] requestBodyIV, byte[] requestBodyKey, byte responseHeader) {
        super(requestBodyIV, requestBodyKey, responseHeader);
    }
}
