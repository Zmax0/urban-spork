package com.urbanspork.test.template;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.ClientHandshake;
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
import io.netty.util.concurrent.EventExecutor;
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
public abstract class UDPTestTemplate extends TestTemplate {

    private static final Logger logger = LoggerFactory.getLogger(UDPTestTemplate.class);
    private static final int[] DST_PORTS = TestUtil.freePorts(2);
    private final EventLoopGroup group = new NioEventLoopGroup();
    protected final EventExecutor executor = new DefaultEventLoop();
    protected final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
    private final Channel channel = initChannel();
    private Consumer<TernaryDatagramPacket> consumer;
    private Future<?> simpleEchoTestServer;
    private Future<?> delayedEchoTestServer;
    protected Future<?> server;
    protected Future<?> client;

    @BeforeAll
    protected void beforeAll() {
        launchUDPTestServer();
    }

    private void launchUDPTestServer() {
        simpleEchoTestServer = service.submit(() -> {
            try {
                SimpleEchoTestServer.launch(DST_PORTS[0]);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        delayedEchoTestServer = service.submit(() -> {
            try {
                DelayedEchoTestServer.launch(DST_PORTS[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(simpleEchoTestServer.isCancelled());
        Assertions.assertFalse(delayedEchoTestServer.isCancelled());
    }

    protected int[] dstPorts() {
        return DST_PORTS;
    }

    protected void handshakeAndSendBytes(ClientConfig config, int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress("localhost", config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        ClientHandshake.Result result = ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        Promise<Void> promise = executor.newPromise();
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
        Assertions.assertTrue(promise.await(DelayedEchoTestServer.MAX_DELAYED_SECOND + 3, TimeUnit.SECONDS));
    }

    @AfterEach
    void cancel() {
        cancel(client, server);
    }

    @AfterAll
    void shutdown() {
        channel.close();
        simpleEchoTestServer.cancel(true);
        delayedEchoTestServer.cancel(true);
        service.shutdown();
        group.shutdownGracefully();
        executor.shutdownGracefully();
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
