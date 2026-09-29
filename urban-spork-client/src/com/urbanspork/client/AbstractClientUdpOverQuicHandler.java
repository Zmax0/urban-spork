package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.util.FutureListeners;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.time.Duration;

public abstract class AbstractClientUdpOverQuicHandler<K> extends AbstractClientUdpRelayHandler<K, QuicStreamChannel> {
    protected final ChannelFuture endpoint;

    protected AbstractClientUdpOverQuicHandler(ClientChannelContext context, Duration keepAlive, EventLoopGroup workerGroup) {
        super(context, keepAlive);
        endpoint = newEndpoint(context, workerGroup);
    }

    protected ChannelFuture newEndpoint(ClientChannelContext context, EventLoopGroup workerGroup) {
        return context.quicEndpoint(workerGroup);
    }

    protected abstract ChannelInitializer<Channel> newOutboundInitializer(K k);

    protected abstract ChannelHandler newInboundHandler(Channel inbound, K k);

    @Override
    protected void newBindingChannel(Channel inbound, K k, Promise<QuicStreamChannel> promise) {
        endpoint.addListener((GenericFutureListener<ChannelFuture>) f1 -> {
            if (f1.isSuccess()) {
                ServerConfig config = context.config();
                InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
                Channel c1 = f1.channel();
                newQuicChannel(c1, serverAddress).addListener((GenericFutureListener<Future<QuicChannel>>) f2 -> {
                    if (!f2.isSuccess()) {
                        c1.close();
                        promise.tryFailure(f2.cause());
                        return;
                    }
                    QuicChannel c2 = f2.getNow();
                    c2.newStreamBootstrap().handler(newOutboundInitializer(k)).create(promise);
                });
                promise.addListener(FutureListeners.success(outbound -> outbound.pipeline().addLast(newInboundHandler(inbound, k)))); // R → L
            } else {
                promise.tryFailure(f1.cause());
            }
        });

    }

    protected Future<QuicChannel> newQuicChannel(Channel endpointChannel, InetSocketAddress serverAddress) {
        return QuicChannel.newBootstrap(endpointChannel).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter()).connect();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        super.handlerRemoved(ctx);
        endpoint.channel().close();
    }
}
