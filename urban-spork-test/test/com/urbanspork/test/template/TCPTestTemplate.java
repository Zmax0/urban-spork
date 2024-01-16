package com.urbanspork.test.template;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestUtil;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TCPTestTemplate extends TestTemplate {
    final EventLoopGroup group = new NioEventLoopGroup();
    protected final DefaultEventLoop executor = new DefaultEventLoop();
    protected final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    final int dstPort = TestUtil.freePort();

    @BeforeAll
    protected void beforeAll() throws ExecutionException, InterruptedException {
        launchEchoTestServer();
    }

    private void launchEchoTestServer() throws InterruptedException, ExecutionException {
        Promise<Channel> promise = executor.newPromise();
        pool.submit(() -> EchoTestServer.launch(dstPort, promise));
        InetSocketAddress address = (InetSocketAddress) promise.await().get().localAddress();
        Assertions.assertEquals(dstPort, address.getPort());
    }

    protected void handshakeAndSendBytes(ClientConfig config) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        ClientHandshake.Result result = ClientHandshake.noAuth(group, Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
        Channel channel = result.sessionChannel();
        int length = ThreadLocalRandom.current().nextInt(0xfff, 0xffff);
        byte[] bytes = Dice.rollBytes(length);
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(
            new FixedLengthFrameDecoder(length),
            new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    if (Arrays.equals(ByteBufUtil.getBytes(msg), bytes)) {
                        promise.setSuccess(null);
                    } else {
                        promise.setFailure(new RuntimeException("Received unexpected msg"));
                    }
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.setFailure(cause);
                }
            });
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(promise.isSuccess(), promise.cause() != null ? promise.cause().getMessage() : "");
    }

    @AfterAll
    protected void afterAll() {
        pool.shutdown();
        executor.shutdownGracefully();
        group.shutdownGracefully();
    }
}
