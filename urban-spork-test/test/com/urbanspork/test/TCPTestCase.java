package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.common.util.Dice;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TCPTestCase {

    private static final int[] PORTS = TestUtil.freePorts(3);
    private static final int DST_PORT = PORTS[2];
    private final ExecutorService service = Executors.newFixedThreadPool(3);
    private final String hostname = "localhost";
    private final ClientConfig config = initConfig();

    @BeforeAll
    void init() {
        service.submit(() -> EchoTestServer.launch(DST_PORT));
        service.submit(() -> Server.launch(config.getServers()));
        service.submit(() -> Client.launch(config));
    }

    @Test
    void testNoAuthConnect() throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress(hostname, DST_PORT);
        Socks5Handshaking.Result result = Socks5Handshaking.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        byte[] bytes = Dice.randomBytes(1024);
        Channel channel = result.sessionChannel();
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                Assertions.assertArrayEquals(ByteBufUtil.getBytes(msg), bytes);
                promise.setSuccess(null);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                promise.setFailure(cause);
            }
        });
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.await().get();
        Assertions.assertTrue(promise.isSuccess());
        channel.eventLoop().shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        service.shutdown();
    }

    private ClientConfig initConfig() {
        ClientConfig config = new ClientConfig();
        config.setPort(PORTS[1]);
        config.setIndex(0);
        config.setServers(List.of(initServerConfig()));
        return config;
    }

    private ServerConfig initServerConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setHost(hostname);
        serverConfig.setPort(PORTS[0]);
        serverConfig.setProtocol(Protocols.shadowsocks);
        serverConfig.setCipher(SupportedCipher.aes_256_gcm);
        serverConfig.setPassword(UUID.randomUUID().toString());
        serverConfig.setNetworks(new Network[]{Network.TCP});
        return serverConfig;
    }
}
