package com.urbanspork.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final ClientChannelContext context;

    ClientSocksConnectHandler(ClientChannelContext context) {
        this.context = context;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        new ClientTcpRelayHandler() {
            @Override
            public Consumer<Channel> outboundReady(Channel inbound) {
                inbound.pipeline().remove(ClientSocksConnectHandler.class);
                return _ -> {};
            }

            @Override
            public ClientRelayHandler.InboundReady inboundReady() {
                return new ClientRelayHandler.InboundReady(
                    c -> c.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort())),
                    c -> c.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
                );
            }
        }.connect(ctx.channel(), InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort()), context);
    }
}