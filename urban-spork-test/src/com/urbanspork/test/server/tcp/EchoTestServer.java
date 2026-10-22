package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class EchoTestServer implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(EchoTestServer.class);
    public static final int PORT = 16802;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerSocketChannel channel;

    private EchoTestServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerSocketChannel channel) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.channel = channel;
    }

    static void main() throws InterruptedException {
        launch(PORT).awaitTermination();
    }

    public static EchoTestServer launch(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ServerSocketChannel channel = (ServerSocketChannel) new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                logger.info("Receive msg and send back");
                                ctx.writeAndFlush(msg);
                            }
                        });
                    }
                })
                .bind(InetAddress.getLoopbackAddress(), port).sync().channel();
            logger.info("Launch echo test server => {}", channel.localAddress());
            return new EchoTestServer(bossGroup, workerGroup, channel);
        } catch (InterruptedException | RuntimeException e) {
            shutdown(workerGroup, bossGroup);
            throw e;
        }
    }

    public InetSocketAddress localAddress() {
        return channel.localAddress();
    }

    public void awaitTermination() throws InterruptedException {
        channel.closeFuture().sync();
    }

    @Override
    public void close() {
        channel.close().syncUninterruptibly();
        shutdown(workerGroup, bossGroup);
    }

    private static void shutdown(EventLoopGroup workerGroup, EventLoopGroup bossGroup) {
        Future<?> workerTerminated = workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerTerminated.syncUninterruptibly();
    }
}
