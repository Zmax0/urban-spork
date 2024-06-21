package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUdpAssociateHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;

@ChannelHandler.Sharable
public class ClientSocksMessageHandler extends SimpleChannelInboundHandler<Socks5Message> {

    public static final ClientSocksMessageHandler INSTANCE = new ClientSocksMessageHandler();

    private ClientSocksMessageHandler() {}

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5Message msg) {
        if (msg instanceof Socks5InitialRequest) {
            ctx.pipeline().replace(Socks5InitialRequestDecoder.class, null, new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (msg instanceof Socks5CommandRequest request) {
            channelRead1(ctx, request);
        } else {
            ctx.close();
        }
    }

    private void channelRead1(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        ChannelPipeline pipeline = ctx.pipeline();
        if (request.type() == Socks5CommandType.CONNECT) {
            pipeline.replace(this, null, new ClientSocksConnectHandler());
            pipeline.remove(Socks5CommandRequestDecoder.class);
            ctx.fireChannelRead(request);
        } else if (request.type() == Socks5CommandType.UDP_ASSOCIATE) {
            pipeline.replace(this, null, ClientUdpAssociateHandler.INSTANCE);
            pipeline.remove(Socks5CommandRequestDecoder.class);
            ctx.fireChannelRead(request);
        } else {
            ctx.write(new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, request.dstAddrType()));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}