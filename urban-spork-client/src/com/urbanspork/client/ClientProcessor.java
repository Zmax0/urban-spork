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
import io.netty.util.internal.StringUtil;

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
            .option(ChannelOption.TCP_NODELAY, true)
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
                localChannel.pipeline().remove(ClientProcessor.this);
                if (future.isSuccess()) {
                    localChannel.pipeline().addLast(new DefaultChannelInboundHandler(future.channel()));
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));
                } else {
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                    logger.error("Connect proxy server failed");
                }
            });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(StringUtil.EMPTY_STRING, cause);
        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
    }

}
