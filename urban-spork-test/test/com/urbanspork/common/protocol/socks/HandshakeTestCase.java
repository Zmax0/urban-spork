package com.urbanspork.common.protocol.socks;

import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

@DisplayName("VMess - Handshake")
class HandshakeTestCase {
    @Test
    void testNoAuthFailed() {
        InetSocketAddress proxyAddress = new InetSocketAddress(TestUtil.freePort());
        InetSocketAddress dstAddress = new InetSocketAddress(TestDice.rollPort());
        Promise<Handshake.Result> promise = Handshake.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress);
        Assertions.assertThrows(ExecutionException.class, promise::get);
    }
}
