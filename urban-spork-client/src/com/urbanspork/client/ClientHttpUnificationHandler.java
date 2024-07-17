package com.urbanspork.client;

import com.urbanspork.common.util.HttpProxyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

@ChannelHandler.Sharable
class ClientHttpUnificationHandler extends SimpleChannelInboundHandler<ByteBuf> {
    static final ClientHttpUnificationHandler INSTANCE = new ClientHttpUnificationHandler();

    private ClientHttpUnificationHandler() {}

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        HttpProxyUtil.Option option = HttpProxyUtil.parseOption(msg);
        InetSocketAddress dstAddress = option.address();
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        if (localAddress.getHostString().equals(dstAddress.getHostString()) && localAddress.getPort() == dstAddress.getPort()) {
            ctx.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes())).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (HttpMethod.CONNECT == option.method()) {
            new HttpsRelayHandler().connect(ctx.channel(), dstAddress);
        } else {
            new HttpRelayHandler(msg.retain()).connect(ctx.channel(), dstAddress);
        }
    }

    private record HttpRelayHandler(ByteBuf msg) implements ClientTcpRelayHandler {
        @Override
        public ChannelHandler inboundHandler() {
            return INSTANCE;
        }

        @Override
        public Consumer<Channel> outboundWriter() {
            return channel -> channel.writeAndFlush(msg);
        }
    }

    private static class HttpsRelayHandler implements ClientTcpRelayHandler {
        private static final byte[] SUCCESS = "HTTP/1.1 200 Connection established\r\n\r\n".getBytes();
        private static final byte[] FAILED = "HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes();

        @Override
        public ChannelHandler inboundHandler() {
            return INSTANCE;
        }

        @Override
        public InboundWriter inboundWriter() {
            return new InboundWriter(
                channel -> channel.writeAndFlush(Unpooled.wrappedBuffer(SUCCESS)),
                channel -> channel.writeAndFlush(Unpooled.wrappedBuffer(FAILED))
            );
        }
    }
}