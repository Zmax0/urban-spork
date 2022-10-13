package com.urbanspork.server;

import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.protocol.ShadowsocksProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
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
                        logger.info("Connect success [id: {}, L: {} - R: /{}]", localChannel.id(), localChannel.localAddress(), remoteAddress.getHostName());
                        ctx.fireChannelRead(msg);
                    } else {
                        logger.error("Connect remote address failed {}", remoteAddress);
                        ChannelCloseUtils.closeOnFlush(ctx.channel());
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }
}
