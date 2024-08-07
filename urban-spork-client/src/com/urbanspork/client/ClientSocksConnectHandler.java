package com.urbanspork.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

import java.net.InetSocketAddress;

class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        new ClientTcpRelayHandler() {
            @Override
            public ChannelHandler inboundHandler() {
                return ClientSocksConnectHandler.this;
            }

            @Override
            public InboundWriter inboundWriter() {
                return new InboundWriter(
                    c -> c.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort())),
                    c -> c.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
                );
            }
        }.connect(ctx.channel(), InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort()));
    }
}