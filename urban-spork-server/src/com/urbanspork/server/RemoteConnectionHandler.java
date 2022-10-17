package com.urbanspork.server;

import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.protocol.ShadowsocksProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteConnectionHandler extends ChannelInboundHandlerAdapter implements ShadowsocksProtocol {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectionHandler.class);

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel localChannel = ctx.channel();
        final InetSocketAddress remoteAddress = decodeAddress((ByteBuf) msg);
        b.group(localChannel.eventLoop())
                .channel(localChannel.getClass())
                .handler(new DefaultChannelInboundHandler(localChannel))
                .connect(remoteAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        logger.info("Connect success [id: {}, L: {} - R: /{}]", localChannel.id(), localChannel.localAddress(), remoteAddress.getHostName());
                        synchronized (localChannel.pipeline()) {
                            localChannel.pipeline().addLast(new DefaultChannelInboundHandler(future.channel()));
                            localChannel.pipeline().remove(RemoteConnectionHandler.this);
                            ctx.fireChannelRead(msg);
                        }
                    } else {
                        logger.error("Connect remote address failed {}", remoteAddress);
                        ChannelCloseUtils.closeOnFlush(ctx.channel());
                    }
                });
    }
}