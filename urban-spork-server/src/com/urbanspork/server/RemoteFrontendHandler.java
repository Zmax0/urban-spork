package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteFrontendHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFrontendHandler.class);

    private static final EventLoopGroup REMOTE_WORKER_GROUP = new NioEventLoopGroup();

    private Object msg;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        InetSocketAddress remoteAddress = localChannel.attr(AttributeKeys.REMOTE_ADDRESS).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(REMOTE_WORKER_GROUP)
                .channel(localChannel.getClass())
                .option(ChannelOption.AUTO_READ, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel remoteChannel) {
                        remoteChannel.pipeline().addLast(new RemoteBackendHandler(localChannel));
                    }
                })
                .connect(remoteAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ChannelPipeline pipeline = localChannel.pipeline();
                        pipeline.addLast(new DefaultChannelInboundHandler(future.channel()));
                        if (pipeline.get(RemoteFrontendHandler.class) != null) {
                            pipeline.remove(RemoteFrontendHandler.this);
                        }
                        ctx.fireChannelRead(msg);
                    } else {
                        logger.error("Connect {} failed", remoteAddress);
                        localChannel.close();
                        ChannelCloseUtils.closeOnFlush(ctx.channel());
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        this.msg = msg;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ReferenceCountUtil.release(msg);
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
