package com.urbanspork.test.tool;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TcpCapture {
    private static final Logger logger = LoggerFactory.getLogger(TcpCapture.class);
    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final List<byte[]> outboundCapture = new ArrayList<>();
    private final int remotePort;
    private final boolean block;
    private final ServerSocketChannel localChannel;
    private final Promise<List<byte[]>> outboundPromise;

    public TcpCapture(int remotePort, boolean block) {
        this.remotePort = remotePort;
        this.block = block;
        this.localChannel = start();
        this.outboundPromise = bossGroup.next().newPromise();
    }

    public List<byte[]> nextOutboundCapture() throws InterruptedException, ExecutionException {
        return outboundPromise.get();
    }

    public ServerSocketChannel getLocalChannel() {
        return localChannel;
    }

    public SocketChannel newRemoteChannel() throws InterruptedException {
        return (SocketChannel) new Bootstrap().group(bossGroup)
            .channel(NioSocketChannel.class)
            .handler(new LoggingHandler() {
                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    TcpCapture.logger.error("{}: {}", ctx.channel(), cause.getMessage());
                    ctx.close();
                }
            })
            .connect(InetAddress.getLoopbackAddress(), remotePort)
            .sync().channel();
    }

    private ServerSocketChannel start() {
        return (ServerSocketChannel) new ServerBootstrap().group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInboundHandlerAdapter() {
                private ChannelPromise promise;

                @Override
                public void handlerAdded(ChannelHandlerContext ctx) {
                    if (block) {
                        return;
                    }
                    promise = ctx.newPromise();
                    Channel inbound = ctx.channel();
                    new Bootstrap().group(inbound.eventLoop())
                        .channel(inbound.getClass())
                        .handler(new DefaultChannelInboundHandler(inbound))
                        .connect(InetAddress.getLoopbackAddress(), remotePort).addListener((ChannelFutureListener) future -> {
                                if (future.isSuccess()) {
                                    Channel outbound = future.channel();
                                    inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound));
                                    promise.setSuccess();
                                } else {
                                    logger.error("Connect target localhost:{} failed", remotePort, future.cause());
                                    promise.setFailure(future.cause());
                                    ctx.close();
                                }
                            }
                        );
                }

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    if (msg instanceof ByteBuf buf) {
                        logger.info("captured outbound: {} bytes", buf.readableBytes());
                        outboundCapture.add(ByteBufUtil.getBytes(buf));
                        if (!outboundPromise.isDone()) {
                            outboundPromise.setSuccess(outboundCapture);
                        }
                        if (block) {
                            return;
                        }
                        if (promise.isDone()) {
                            ctx.fireChannelRead(buf);
                        } else {
                            promise.addListener(_ -> ctx.fireChannelRead(buf));
                        }
                    } else {
                        ctx.fireChannelRead(msg);
                    }
                }
            })
            .bind(0).syncUninterruptibly().channel();
    }
}
