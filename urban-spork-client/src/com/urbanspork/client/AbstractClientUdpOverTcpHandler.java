package com.urbanspork.client;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

public abstract class AbstractClientUdpOverTcpHandler<K> extends AbstractClientUdpRelayHandler<K> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientUdpOverTcpHandler.class);
    private final EventLoopGroup workerGroup;

    protected AbstractClientUdpOverTcpHandler(ServerConfig config, Duration keepAlive, EventLoopGroup workerGroup) {
        super(config, keepAlive);
        this.workerGroup = workerGroup;
    }

    protected abstract ChannelInitializer<Channel> newOutboundInitializer(K key);

    protected abstract ChannelHandler newInboundHandler(Channel inboundChannel, K key);

    @Override
    protected Channel newBindingChannel(Channel inboundChannel, K key) {
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        return new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(newOutboundInitializer(key))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(newInboundHandler(inboundChannel, key)); // R → L
                    inboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                } else {
                    logger.error("connect relay server {} failed", serverAddress);
                }
            }).syncUninterruptibly().channel();
    }

    protected void addWebSocketHandler(Channel channel) throws URISyntaxException {
        if (ClientRelayHandler.addWebSocketHandlers(channel, config)) {
            channel.pipeline().addLast(new WebSocketCodec());
        }
    }

    static class WebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private ChannelPromise promise;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            promise = ctx.newPromise();
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(msg.retain());
            if (!promise.isDone()) {
                promise.addListener(f -> ctx.writeAndFlush(frame));
            } else {
                out.add(frame);
            }
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {
            out.add(msg.retain().content());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                promise.setSuccess();
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}
