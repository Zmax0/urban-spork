package com.urbanspork.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.protocol.ShadowsocksProtocolEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) throws Exception {
        Channel localChannel = ctx.channel();
        InetSocketAddress serverAddress = localChannel.attr(AttributeKeys.SERVER_ADDRESS).get();
        logger.debug("Connect proxy server {}", serverAddress);
        new Bootstrap()
            .group(localChannel.eventLoop())
            .channel(localChannel.getClass())
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel remoteChannel) throws Exception {
                    remoteChannel.attr(AttributeKeys.KEY).set(localChannel.attr(AttributeKeys.KEY).get());
                    remoteChannel.attr(AttributeKeys.CIPHER).set(localChannel.attr(AttributeKeys.CIPHER).get());
                    remoteChannel.attr(AttributeKeys.REQUEST).set(request);
                    remoteChannel.pipeline()
                        .addLast(new ShadowsocksCipherCodec())
                        .addLast(new ShadowsocksProtocolEncoder())
                        .addLast(new DefaultChannelInboundHandler(localChannel));
                }
            }).connect(serverAddress).addListener((ChannelFutureListener) future -> {
                localChannel.pipeline().remove(ClientSocksConnectHandler.this);
                if (future.isSuccess()) {
                    localChannel.pipeline().addLast(new DefaultChannelInboundHandler(future.channel()));
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));
                } else {
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                    logger.error("Connect proxy server {} failed", serverAddress);
                }
            });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " ~>", cause);
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
