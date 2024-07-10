package com.urbanspork.client;

import com.urbanspork.common.util.HttpProxyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;

import java.util.function.Consumer;

@ChannelHandler.Sharable
class HttpPortUnificationHandler extends SimpleChannelInboundHandler<ByteBuf> {
    static final HttpPortUnificationHandler INSTANCE = new HttpPortUnificationHandler();

    private HttpPortUnificationHandler() {}

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        HttpProxyUtil.Option option = HttpProxyUtil.parseOption(msg);
        if (HttpMethod.GET == option.method()) {
            new HttpConnectHandler(msg.retain()).connect(ctx.channel(), option.address());
        } else {
            new HttpsConnectHandler().connect(ctx.channel(), option.address());
        }
    }

    private record HttpConnectHandler(ByteBuf msg) implements ClientConnectHandler {
        @Override
        public ChannelHandler inboundHandler() {
            return INSTANCE;
        }

        @Override
        public InboundWriter inboundWriter() {
            return new InboundWriter(channel -> {}, channel -> {});
        }

        @Override
        public Consumer<Channel> outboundWriter() {
            return channel -> channel.writeAndFlush(msg);
        }
    }

    private static class HttpsConnectHandler implements ClientConnectHandler {
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

        @Override
        public Consumer<Channel> outboundWriter() {
            return channel -> {};
        }
    }
}