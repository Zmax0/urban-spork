package com.urbanspork.test.client;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Socks5UdpTestClient extends TestClientTemplate {
    private static final Logger logger = LoggerFactory.getLogger(Socks5UdpTestClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(10);
    private static final int[] RETRY_DELAYS_SECONDS = {10, 10, 10, 15, 25, 40, 60};

    static void main() throws IOException {
        new Socks5UdpTestClient().launch();
    }

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
    private final InetSocketAddress dstAddress1 = InetSocketAddress.createUnresolved(dstAddress, SimpleEchoTestServer.PORT);
    private final InetSocketAddress dstAddress2 = InetSocketAddress.createUnresolved(dstAddress, DelayedEchoTestServer.PORT);

    private volatile UdpAssociation association;
    private volatile boolean stopping;
    private int reconnectAttempts = 1;
    private ScheduledFuture<?> reconnectTask;

    private void launch() throws IOException {
        logger.info("Proxy address: {}", proxyAddress);
        logger.info("Destination address: {}, {}", dstAddress1, dstAddress2);
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            try {
                association = newAssociation(group);
                logger.info("Bind local address {}", association.channel().localAddress());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                logger.warn("Initial association failed", e);
            }

            scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    if (stopping || association != null && association.isActive()) {
                        return;
                    }
                    if (reconnectTask != null && !reconnectTask.isDone()) {
                        return;
                    }
                    int delaySeconds = currentDelay(reconnectAttempts);
                    logger.warn("association is unavailable, retrying in {} seconds", delaySeconds);
                    reconnectTask = scheduledExecutorService.schedule(retryChannel(group), delaySeconds, TimeUnit.SECONDS);
                }, 0, HEALTH_CHECK_INTERVAL.getSeconds(), TimeUnit.SECONDS
            );

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            if (association != null && association.isActive()) {
                logger.info("Enter text (quit to end)");
            }
            for (; ; ) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                if (association == null || !association.isActive()) {
                    logger.warn("association is not ready, message was skipped");
                    continue;
                }
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                Socks5CommandResponse response1 = association.result1().response();
                Socks5CommandResponse response2 = association.result2().response();
                sendMsg(association.channel(), dstAddress1, new InetSocketAddress(response1.bndAddr(), response1.bndPort()), bytes);
                sendMsg(association.channel(), dstAddress2, new InetSocketAddress(response2.bndAddr(), response2.bndPort()), bytes);
            }
        } finally {
            stopping = true;
            scheduledExecutorService.shutdownNow();
            if (association != null) {
                association.close();
            }
            group.shutdownGracefully();
        }
    }

    private Runnable retryChannel(EventLoopGroup group) {
        return () -> {
            if (stopping || association != null && association.isActive()) {
                return;
            }

            int attempt = reconnectAttempts++;
            logger.warn("Reconnect association, attempt {}", attempt);
            try {
                UdpAssociation association = newAssociation(group);
                UdpAssociation temp = this.association;
                this.association = association;
                reconnectAttempts = 1;
                if (temp != null) {
                    temp.close();
                }
                logger.info("Reconnected and bound local address {}", association.channel().localAddress());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                int delaySeconds = currentDelay(reconnectAttempts);
                if (!stopping) {
                    reconnectTask = scheduledExecutorService.schedule(retryChannel(group), delaySeconds, TimeUnit.SECONDS);
                }
            }
        };
    }

    private UdpAssociation newAssociation(EventLoopGroup group) throws InterruptedException, ExecutionException, TimeoutException {
        HandshakeResult<Socks5CommandResponse> result1 = null;
        HandshakeResult<Socks5CommandResponse> result2;
        ChannelFuture channelFuture = new Bootstrap().group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        new SimpleChannelInboundHandler<DatagramPacketWrapper>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
                                ByteBuf content = msg.packet().content();
                                InetSocketAddress dst = msg.server();
                                logger.info("Receive msg {} - {}", dst, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    );
                }
            })
            .bind(0);
        Channel udpChannel = channelFuture.channel();
        try {
            if (!channelFuture.await(CONNECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                throw new TimeoutException("Timed out binding local UDP channel");
            }
            if (!channelFuture.isSuccess()) {
                throw new ExecutionException(channelFuture.cause());
            }
            result1 = handshake(group, dstAddress1);
            result2 = handshake(group, dstAddress2);
            logger.info("Associate ports: [{}, {}]", result1.response().bndPort(), result2.response().bndPort());
            return new UdpAssociation(udpChannel, result1, result2);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (udpChannel != null) {
                udpChannel.close();
            }
            if (result1 != null) {
                result1.channel().close();
            }
            throw e;
        }
    }

    private HandshakeResult<Socks5CommandResponse> handshake(EventLoopGroup group, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException, TimeoutException {
        return Handshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get(CONNECT_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
    }

    private int currentDelay(int attempt) {
        return RETRY_DELAYS_SECONDS[Math.min(attempt - 1, RETRY_DELAYS_SECONDS.length - 1)];
    }

    private static void sendMsg(Channel channel, InetSocketAddress dstAddress, InetSocketAddress socksAddress, byte[] bytes) {
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), dstAddress);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(data, socksAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
    }

    private record UdpAssociation(
        Channel channel,
        HandshakeResult<Socks5CommandResponse> result1,
        HandshakeResult<Socks5CommandResponse> result2
    ) {
        private boolean isActive() {
            return channel.isActive() && result1.channel().isActive() && result2.channel().isActive();
        }

        private void close() {
            channel.close();
            result1.channel().close();
            result2.channel().close();
        }
    }
}
