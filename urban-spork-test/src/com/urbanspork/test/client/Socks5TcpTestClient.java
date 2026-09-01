package com.urbanspork.test.client;

import com.urbanspork.common.protocol.HandshakeResult;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class Socks5TcpTestClient extends TcpTestClientTemplate<Socks5CommandResponse> {
    static void main() throws IOException, ExecutionException, InterruptedException {
        new Socks5TcpTestClient().launch();
    }

    @Override
    protected HandshakeResult<Socks5CommandResponse> handshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        HandshakeResult<Socks5CommandResponse> result = Socks5Handshake.noAuth(bossGroup, Socks5CommandType.CONNECT, proxyAddress, dstAddress).get();
        if (!result.response().status().isSuccess()) {
            result.channel().close();
            throw new ExecutionException(new IllegalStateException("Unsuccessful response status: " + result.response().status()));
        }
        return result;
    }
}
