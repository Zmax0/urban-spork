package com.urbanspork.server;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;
import com.urbanspork.common.DefaultChannelInboundHandler;

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
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.StringUtil;

public class ServerProcessor extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerProcessor.class);

    private final Cumulator cumulator = ByteToMessageDecoder.MERGE_CUMULATOR;

    private boolean first;

    private ByteBuf cumulation;

    private Channel remoteChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf data = (ByteBuf) msg;
            first = cumulation == null;
            if (first) {
                cumulation = data;
            } else {
                cumulator.cumulate(ctx.alloc(), cumulation, data);
            }
            Channel localChannel = ctx.channel();
            InetSocketAddress remoteAddress = localChannel.attr(Attributes.REMOTE_ADDRESS).get();
            if (remoteAddress != null) {
                if (remoteChannel == null) {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap
                        .group(localChannel.eventLoop())
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30 * 1000)
                        .option(ChannelOption.SO_RCVBUF, 32 * 1024)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                    .addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS) {
                                        @Override
                                        protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
                                            logger.debug("{} state: {}", remoteAddress, state);
                                            ServerProcessor.this.close();
                                            localChannel.close();
                                            return super.newIdleStateEvent(state, first);
                                        }
                                    })
                                    .addLast(new DefaultChannelInboundHandler(localChannel));
                            }
                        })
                        .connect(remoteAddress)
                        .addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                remoteChannel = future.channel();
                                logger.info("Connect channel {}", remoteChannel);
                                if (cumulation != null) {
                                    remoteChannel.write(cumulation);
                                    cumulation = null;
                                    remoteChannel.flush();
                                }
                            } else {
                                throw new IllegalStateException("Connect " + remoteAddress + " failed");
                            }
                        });
                }
                if (cumulation != null && remoteChannel != null) {
                    remoteChannel.write(cumulation);
                    cumulation = null;
                    remoteChannel.flush();
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.close();
        ctx.close();
        logger.error(StringUtil.EMPTY_STRING, cause);
    }

    private void close() {
        if (cumulation != null) {
            cumulation = null;
        }
        if (remoteChannel != null) {
            logger.info("Close channel {}", remoteChannel);
            remoteChannel.close();
        }
    }
}
