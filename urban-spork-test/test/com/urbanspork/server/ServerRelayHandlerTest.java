package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerRelayHandlerTest {
    @Test
    void testReadUnexpectedMsg() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerRelayHandler(config));
        ByteBuf msg = Unpooled.wrappedBuffer(Dice.rollBytes(10));
        Assertions.assertThrows(NullPointerException.class, () -> channel.writeInbound(msg));
    }

    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerRelayHandler(config));
        channel.writeInbound(new RelayingPayload<>(new InetSocketAddress(0), Unpooled.wrappedBuffer(Dice.rollBytes(10))));
        Assertions.assertFalse(channel.isActive());
    }

    @Test
    void testClientClose() throws ExecutionException, InterruptedException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            CompletableFuture<ServerSocketChannel> promise = new CompletableFuture<>();
            pool.submit(() -> EchoTestServer.launch(0, promise));
            ServerSocketChannel echoTestServer = promise.get();
            EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            Channel channel = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelHandlerAdapter() {
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) {
                        ctx.pipeline().addLast(new ServerRelayHandler(config)).fireChannelRead(new RelayingPayload<>(echoTestServer.localAddress(), Unpooled.wrappedBuffer(Dice.rollBytes(10))));
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .bind(0).sync().channel();
            Assertions.assertFalse(channel.isActive());
            echoTestServer.close();
        }
    }
}
