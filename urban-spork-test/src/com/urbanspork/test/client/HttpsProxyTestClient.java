package com.urbanspork.test.client;

import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.protocol.http.ClientHandshake;
import io.netty.handler.codec.http.HttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class HttpsProxyTestClient extends TcpTestClientTemplate<HttpResponse> {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        new HttpsProxyTestClient().launch();
    }

    @Override
    protected HandshakeResult<HttpResponse> handshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        return ClientHandshake.https(bossGroup, proxyAddress, dstAddress).get();
    }
}
