package com.urbanspork.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.Attributes;
import com.urbanspork.common.DefaultChannelInboundHandler;
import com.urbanspork.protocol.ShadowsocksClientProtocolCodec;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public class ClientProcessor extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientProcessor.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest msg) throws Exception {
        Channel localChannel = ctx.channel();
        InetSocketAddress serverAddress = localChannel.attr(Attributes.SERVER_ADDRESS).get();
        logger.debug("Connect proxy server {}", serverAddress);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(localChannel.eventLoop())
            .channel(localChannel.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel remoteChannel) throws Exception {
                    remoteChannel.attr(Attributes.KEY).set(localChannel.attr(Attributes.KEY).get());
                    remoteChannel.attr(Attributes.CIPHER).set(localChannel.attr(Attributes.CIPHER).get());
                    remoteChannel.attr(Attributes.REQUEST).set(msg);
                    remoteChannel.pipeline()
                        .addLast(new ShadowsocksCipherCodec())
                        .addLast(new ShadowsocksClientProtocolCodec())
                        .addLast(new DefaultChannelInboundHandler(localChannel));
                }
            }).connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));
                    localChannel.pipeline().remove(ClientProcessor.this).addLast(new DefaultChannelInboundHandler(future.channel()));
                } else {
                    logger.error("Connect proxy server failed");
                }
            });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Channel [{}] ERROR", ctx.channel().id(), cause);
    }

}
