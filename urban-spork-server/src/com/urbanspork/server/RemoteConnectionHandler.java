package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectionHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel localChannel = ctx.channel();
        InetSocketAddress remoteAddress = localChannel.attr(AttributeKeys.REMOTE_ADDRESS).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(localChannel.eventLoop())
                .channel(localChannel.getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel remoteChannel) {
                        remoteChannel.pipeline().addLast(new DefaultChannelInboundHandler(localChannel));
                    }
                })
                .connect(remoteAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ChannelPipeline pipeline = localChannel.pipeline();
                        pipeline.addLast(new DefaultChannelInboundHandler(future.channel()));
                        if (pipeline.get(RemoteConnectionHandler.class) != null) {
                            pipeline.remove(RemoteConnectionHandler.this);
                        }
                        ctx.fireChannelRead(msg);
                    } else {
                        logger.error("Connect remote address failed {}", remoteAddress);
                        ChannelCloseUtils.closeOnFlush(ctx.channel());
                    }
                });
    }

}
