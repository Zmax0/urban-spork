package com.urbanspork.client;

import com.urbanspork.common.channel.ChannelCloseUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;

@ChannelHandler.Sharable
public class ClientSocksMessageHandler extends SimpleChannelInboundHandler<SocksMessage> {

    public static final ClientSocksMessageHandler INSTANCE = new ClientSocksMessageHandler();

    private ClientSocksMessageHandler() {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        if (SocksVersion.SOCKS5 == msg.version()) {
            if (msg instanceof Socks5InitialRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            } else if (msg instanceof Socks5PasswordAuthRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            } else if (msg instanceof Socks5CommandRequest request) {
                if (request.type() == Socks5CommandType.CONNECT) {
                    ctx.pipeline().addLast(new ClientSocksConnectHandler());
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(request);
                } else {
                    ctx.close();
                }
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
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