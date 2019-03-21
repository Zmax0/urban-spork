package com.urbanspork.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;

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
                if (socksCommandRequest.type() == Socks5CommandType.CONNECT) {
                    ctx.pipeline().addLast(new ClientProcessor()).remove(this);
                    ctx.fireChannelRead(socksCommandRequest);
                } else {
                    ctx.close();
                }
            } else {
                ctx.close();
            }
            break;
        case UNKNOWN:
        default:
            ctx.close();
            break;
        }
    }

}