package com.urbanspork.server;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.common.Attributes;
import com.urbanspork.key.ShadowsocksKey;

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

public class ServerProcessor extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerProcessor.class);

    private ShadowsocksCipher cipher;
    private ShadowsocksKey key;
    private Channel remoteChannel;
    private ByteBuf buff;

    public ServerProcessor(ChannelHandlerContext ctx, ByteBuf buff) {
        Channel channel = ctx.channel();
        this.cipher = channel.attr(Attributes.CIPHER).get();
        this.key = channel.attr(Attributes.KEY).get();
        this.buff = buff;
        init(channel);
    }

    private void init(Channel localChannel) {
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
                    remoteChannel.pipeline().addLast(new ServerReceivedHandler(localChannel, buff.retain()));
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
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof ByteBuf) {
                ByteBuf data = (ByteBuf) msg;
                byte[] decrypt = cipher.decrypt(ByteBufUtil.getBytes(data), key);
                if (remoteChannel == null) {
                    buff.writeBytes(decrypt);
                } else {
                    remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(decrypt));
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
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
            buff = null;
        }
    }

}
