package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.common.util.Dice;
import com.urbanspork.server.Server;
import com.urbanspork.test.TestUtil;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TCPTestTemplate {

    final DefaultEventLoop executor = new DefaultEventLoop();
    final ExecutorService pool = Executors.newFixedThreadPool(1);
    final int dstPort = TestUtil.freePort();

    @BeforeAll
    protected void beforeAll() throws ExecutionException, InterruptedException {
        launchEchoTestServer();
    }

    protected void launchEchoTestServer() throws InterruptedException, ExecutionException {
        Promise<Channel> promise = new DefaultPromise<>(executor);
        pool.submit(() -> EchoTestServer.launch(dstPort, promise));
        InetSocketAddress address = (InetSocketAddress) promise.await().get().localAddress();
        Assertions.assertEquals(dstPort, address.getPort());
    }

    protected void launchClient(ExecutorService service, ClientConfig config) throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
    }

    protected void launchServer(ExecutorService service, List<ServerConfig> configs) throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.get(0).getPort(), promise.await().get().get(0).localAddress().getPort());
    }

    protected void handshakeAndSendBytes(ClientConfig config) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        Handshake.Result result = Handshake.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        int length = ThreadLocalRandom.current().nextInt(0xfff, 0xffff);
        byte[] bytes = Dice.rollBytes(length);
        Channel channel = result.sessionChannel();
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(
            new FixedLengthFrameDecoder(length),
            new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    if (Arrays.equals(ByteBufUtil.getBytes(msg), bytes)) {
                        promise.setSuccess(null);
                    } else {
                        promise.setFailure(AssertionFailureBuilder.assertionFailure().message("Received unexpected msg").build());
                    }
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.setFailure(cause);
                }
            });
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(promise.isSuccess());
        channel.eventLoop().shutdownGracefully();
    }

    @AfterAll
    protected void afterAll() {
        pool.shutdownNow();
        executor.shutdownGracefully();
    }
}
