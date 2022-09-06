package com.urbanspork.client;

import com.urbanspork.common.channel.ChannelCloseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;

public class ClientSocksMessageHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        switch (msg.version()) {
            case SOCKS5:
                if (msg instanceof Socks5InitialRequest) {
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                } else if (msg instanceof Socks5CommandRequest request) {
                    if (request.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new ClientSocksConnectHandler()).remove(this);
                        ctx.fireChannelRead(request);
                    } else {
                        ctx.write(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                    }
                } else if (msg instanceof Socks5PasswordAuthRequest) {
                    ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                } else {
                    ctx.close();
                }
                break;
            case SOCKS4a, UNKNOWN:
            default:
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }
}