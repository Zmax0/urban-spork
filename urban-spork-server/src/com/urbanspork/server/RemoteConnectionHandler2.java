package com.urbanspork.server;

import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.protocol.ShadowsocksProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteConnectionHandler2 extends ChannelInboundHandlerAdapter implements ShadowsocksProtocol {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectionHandler2.class);

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel localChannel = ctx.channel();
        final InetSocketAddress remoteAddress = decodeAddress((ByteBuf) msg);
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (FutureListener<Channel>) future -> {
                    final Channel outboundChannel = future.get();
                    outboundChannel.writeAndFlush(msg).addListener(outboundFuture -> {
                                synchronized (localChannel.pipeline()) {
                                    localChannel.pipeline().addLast(new DefaultChannelInboundHandler(outboundChannel));
                                    outboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(localChannel));
                                    localChannel.pipeline().remove(RemoteConnectionHandler2.this);
                                }
                            }
                    );
                });
        b.group(localChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RemotePromiseHandler(promise))
                .connect(remoteAddress).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        logger.info("Connect success [id: {}, L: {} - R: /{}]", localChannel.id(), localChannel.localAddress(), remoteAddress.getHostName());
                    } else {
                        logger.error("Connect proxy server {} failed", remoteAddress);
                        ChannelCloseUtils.closeOnFlush(localChannel);
                    }
                });
    }
}