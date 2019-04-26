package com.urbanspork.server;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksKey;
import com.urbanspork.common.Attributes;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

public class RemoteConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectHandler.class);

    private Channel remoteChannel;
    private ByteBuf buff;

    public RemoteConnectHandler(Channel localChannel, ByteBuf buff) {
        this.buff = buff;
        connect(localChannel);
    }

    private void connect(Channel localChannel) {
        InetSocketAddress remoteAddress = localChannel.attr(Attributes.REMOTE_ADDRESS).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(localChannel.eventLoop())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(5))
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel remoteChannel) throws Exception {
                    remoteChannel.pipeline().addLast(new RemoteReceiveHandler(localChannel, buff));
                }
            })
            .connect(remoteAddress)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    remoteChannel = future.channel();
                } else {
                    throw new IllegalStateException("Connect " + remoteAddress + " failed");
                }
            });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            try {
                Channel channel = ctx.channel();
                ShadowsocksCipher cipher = channel.attr(Attributes.CIPHER).get();
                ShadowsocksKey key = channel.attr(Attributes.KEY).get();
                byte[] decrypt = cipher.decrypt(ByteBufUtil.getBytes((ByteBuf) msg), key);
                if (remoteChannel == null) {
                    buff.writeBytes(decrypt);
                } else {
                    remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(decrypt));
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} close", ctx.channel());
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
        if (remoteChannel != null) {
            remoteChannel.close();
        }
        if (buff != null) {
            buff.clear();
            buff = null;
        }
    }

}