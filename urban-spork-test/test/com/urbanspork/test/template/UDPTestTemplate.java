package com.urbanspork.test.template;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.function.Consumer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UDPTestTemplate extends TestTemplate {

    private static final Logger logger = LoggerFactory.getLogger(UDPTestTemplate.class);
    private static final int[] DST_PORTS = TestUtil.freePorts(2);
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ExecutorService service = Executors.newFixedThreadPool(2);
    private final Channel channel = initChannel();
    private Consumer<TernaryDatagramPacket> consumer;

    @BeforeAll
    protected void beforeAll() {
        launchUDPTestServer();
    }

    private void launchUDPTestServer() {
        Future<?> future1 = service.submit(() -> {
            try {
                SimpleEchoTestServer.launch(DST_PORTS[0]);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Assertions.assertFalse(future1.isCancelled());
        Future<?> future2 = service.submit(() -> {
            try {
                DelayedEchoTestServer.launch(DST_PORTS[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(future2.isCancelled());
    }

    protected int[] dstPorts() {
        return DST_PORTS;
    }

    protected void handshakeAndSendBytes(ClientConfig config, int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress("localhost", config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        Handshake.Result result = Handshake.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Void> promise = new DefaultPromise<>(executor);
        consumer = msg -> {
            if (dstAddress.equals(msg.third())) {
                promise.setSuccess(null);
            } else {
                promise.setFailure(AssertionFailureBuilder.assertionFailure().message("Not equals").build());
            }
        };
        String str = TestDice.rollString();
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, proxyAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
        Assertions.assertTrue(promise.await(DelayedEchoTestServer.MAX_DELAYED_SECOND + 1, TimeUnit.SECONDS));
        executor.shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        group.shutdownGracefully();
        service.shutdownNow();
    }

    private Channel initChannel() {
        return new Bootstrap().group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        new SimpleChannelInboundHandler<TernaryDatagramPacket>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
                                logger.info("Receive msg {}", msg);
                                consumer.accept(msg);
                            }
                        }
                    );
                }
            })
            .bind(0).syncUninterruptibly().channel();
    }
}
