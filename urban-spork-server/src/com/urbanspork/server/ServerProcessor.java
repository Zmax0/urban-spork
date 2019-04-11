package com.urbanspork.server;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;
import com.urbanspork.common.CachedChannelInboundHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ServerProcessor extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerProcessor.class);

    private boolean first;

    private ByteBuf cumulation;

    private Channel remoteChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel localChannel = ctx.channel();
        InetSocketAddress remoteAddress = localChannel.attr(Attributes.REMOTE_ADDRESS).get();
        if (msg instanceof ByteBuf && remoteAddress != null) {
            ByteBuf data = (ByteBuf) msg;
            if (remoteChannel == null) {
                first = cumulation == null;
                if (first) {
                    cumulation = data;
                } else {
                    ByteToMessageDecoder.MERGE_CUMULATOR.cumulate(ctx.alloc(), cumulation, data);
                }
                Bootstrap bootstrap = new Bootstrap();
                bootstrap
                    .group(localChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(5))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel remoteChannel) throws Exception {
                            remoteChannel.pipeline().addLast(new CachedChannelInboundHandler(localChannel, cumulation.retain()));
                        }
                    })
                    .connect(remoteAddress)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            remoteChannel = future.channel();
                            logger.info("Connect channel {}", remoteChannel);
                        } else {
                            throw new IllegalStateException("Connect " + remoteAddress + " failed");
                        }
                    });
            } else {
                remoteChannel.writeAndFlush(msg);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Channel {} close", ctx.channel());
        ctx.close();
        release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Channel " + ctx.channel() + " error, cause: ", cause);
        ctx.channel().close();
        release();
    }

    private void release() {
        if (cumulation != null) {
            cumulation = null;
        }
        if (remoteChannel != null) {
            remoteChannel.close();
        }
    }

}
