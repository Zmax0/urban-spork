package com.urbanspork.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;

public class ClientShakeHandsHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
        case SOCKS5:
            if (socksRequest instanceof Socks5InitialRequest) {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            } else if (socksRequest instanceof Socks5CommandRequest) {
                Socks5CommandRequest socksCommandRequest = (Socks5CommandRequest) socksRequest;
                Socks5CommandType socks5CommandType = socksCommandRequest.type();
                if (socks5CommandType == Socks5CommandType.CONNECT) {
                    ctx.pipeline().addLast(new ClientProcessor()).remove(this);
                    ctx.fireChannelRead(socksRequest);
                } else {
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                }
            } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
                ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            } else {
                ctx.close();
            }
            break;
        case SOCKS4a:
        case UNKNOWN:
            ctx.close();
            break;
        }
    }

}