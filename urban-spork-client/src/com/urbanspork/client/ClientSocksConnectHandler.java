package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.protocol.ShadowsocksProtocolEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel localChannel = ctx.channel();
        InetSocketAddress serverAddress = localChannel.attr(AttributeKeys.SERVER_ADDRESS).get();
        b.group(localChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel remoteChannel) {
                        remoteChannel.attr(AttributeKeys.REQUEST).set(request);
                        remoteChannel.pipeline()
                                .addLast(new ShadowsocksCipherCodec(localChannel.attr(AttributeKeys.CIPHER).get(), localChannel.attr(AttributeKeys.KEY).get()))
                                .addLast(new ShadowsocksProtocolEncoder())
                                .addLast(new DefaultChannelInboundHandler(localChannel));
                    }
                }).connect(serverAddress).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        localChannel.pipeline()
                                .remove(ClientSocksConnectHandler.this)
                                .addLast(new DefaultChannelInboundHandler(future.channel()));
                        localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));
                    } else {
                        localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                        logger.error("Connect proxy server {} failed", serverAddress);
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
