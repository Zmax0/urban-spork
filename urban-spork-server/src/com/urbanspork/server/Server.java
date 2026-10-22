package com.urbanspork.server;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.udp.UdpRelayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.quic.QuicServerCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Server {
    /*
     * Whoever starts it owns it and closes it.
     */

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main() {
        List<ServerConfig> configs = ConfigHandler.DEFAULT.read().getServers();
        Runtime runtime = new Runtime();
        try (Instances instances = launch(runtime, configs)) {
            instances.awaitTermination();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            runtime.close();
        }
    }

    public static Instances launch(Runtime runtime, List<ServerConfig> configs) throws InterruptedException {
        Context context = Context.newCheckReplayInstance();
        List<Instance> servers = new ArrayList<>(configs.size());
        try {
            for (ServerConfig config : configs) {
                servers.add(start(runtime.parentGroup(), runtime.childGroup(), new ServerChannelContext(config, context)));
            }
            return new Instances(servers, context);
        } catch (InterruptedException e) {
            logger.warn("Interrupt main launch thread");
            close0(servers, context);
            throw e;
        } catch (Throwable e) {
            logger.error("Launch server failed", e);
            close0(servers, context);
            throw new RuntimeException(e);
        }
    }

    private static void close0(List<Instance> servers, Context context) {
        servers.forEach(Instance::close);
        context.release();
    }

    private static Instance start(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerChannelContext context) throws InterruptedException {
        ServerSocketChannel tcp = startTcp(bossGroup, workerGroup, context);
        Optional<DatagramChannel> udp = startUdp(workerGroup, context);
        return new Instance(tcp, udp, context);
    }

    private static ServerSocketChannel startTcp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerChannelContext context) throws InterruptedException {
        ServerConfig config = context.config();
        ServerSocketChannel tcp = (ServerSocketChannel) new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ServerInitializer(context))
            .bind(config.getPort())
            .sync().channel();
        config.setPort(tcp.localAddress().getPort());
        logger.info("Running a tcp server => {}", config);
        return tcp;
    }

    private static Optional<DatagramChannel> startUdp(EventLoopGroup group, ServerChannelContext context) throws InterruptedException {
        ServerConfig config = context.config();
        if (Protocol.shadowsocks == config.getProtocol() && config.udpEnabled()) {
            DatagramChannel udp = (DatagramChannel) new Bootstrap().group(group).channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new UdpRelayCodec(config, Mode.Server, context.userManager()),
                            new ServerUdpRelayHandler(config.getPacketEncoding()),
                            new ExceptionHandler(config)
                        );
                    }
                })
                .bind(config.getPort()).sync().channel();
            logger.info("Running a udp server => {}", config);
            return Optional.of(udp);
        } else if (config.quicEnabled()) {
            return startQuic(group, context);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<DatagramChannel> startQuic(EventLoopGroup boosGroup, ServerChannelContext context) throws InterruptedException {
        ServerConfig config = context.config();
        QuicSslContext quicSslContext = context.quicContext();
        if (quicSslContext == null) {
            return Optional.empty(); // required security setting not present
        }
        ChannelHandler codec = new QuicServerCodecBuilder()
            .sslContext(quicSslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(0xfffff)
            .initialMaxStreamDataBidirectionalLocal(0xfffff)
            .initialMaxStreamDataBidirectionalRemote(0xfffff)
            .initialMaxStreamsBidirectional(100)
            .initialMaxStreamsUnidirectional(100)
            .streamHandler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ServerInitializer.addProtocolHandlers(ch, context);
                }
            })
            .build();
        DatagramChannel channel = (DatagramChannel) new Bootstrap()
            .group(boosGroup)
            .channel(NioDatagramChannel.class)
            .handler(codec)
            .bind(config.getPort())
            .sync()
            .channel();
        logger.info("Running a quic server => {}", config);
        return Optional.of(channel);
    }

    public record Instance(ServerSocketChannel tcp, Optional<DatagramChannel> udp, ServerChannelContext context) implements Closeable {
        @Override
        public void close() {
            udp.ifPresent(c -> c.close().syncUninterruptibly());
            tcp.close().syncUninterruptibly();
        }
    }

    public record Instances(List<Instance> servers, Context context) implements Closeable {

        public Instances {
            servers = List.copyOf(servers);
        }

        public void awaitTermination() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(servers.stream().mapToInt(server -> server.udp().isPresent() ? 2 : 1).sum());
            for (Instance server : servers) {
                server.tcp().closeFuture().addListener(_ -> latch.countDown());
                server.udp().ifPresent(udp -> udp.closeFuture().addListener(_ -> latch.countDown()));
            }
            latch.await();
            logger.info("Server is terminated");
        }

        @Override
        public void close() {
            close0(servers, context);
        }
    }
}
