package com.urbanspork.client;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
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
    private final ChannelFuture endpoint;

    protected AbstractClientUdpOverQuicHandler(ClientChannelContext context, Duration keepAlive, EventLoopGroup workerGroup) {
        super(context, keepAlive);
        endpoint = ClientRelayHandler.quicEndpoint(context.config(), workerGroup);
    }

    protected abstract ChannelInitializer<Channel> newOutboundInitializer(K k);

    protected abstract ChannelHandler newInboundHandler(Channel inbound, K k);

    @Override
    protected void newBindingChannel(Channel inbound, K k, Promise<QuicStreamChannel> promise) {
        endpoint.addListener((GenericFutureListener<ChannelFuture>) f1 -> {
            if (f1.isSuccess()) {
                ServerConfig config = context.config();
                InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
                QuicChannel.newBootstrap(f1.channel()).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter()).connect().addListener((Future<QuicChannel> f2) -> {
                    if (!f2.isSuccess()) {
                        promise.tryFailure(f2.cause());
                        return;
                    }
                    QuicChannel quicChannel = f2.getNow();
                    quicChannel.newStreamBootstrap().handler(newOutboundInitializer(k)).create(promise);
                });
                promise.addListener(FutureListeners.success(outbound -> {
                    outbound.pipeline().addLast(newInboundHandler(inbound, k)); // R → L
                    inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                }));
            } else {
                promise.tryFailure(f1.cause());
            }
        });

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        super.handlerRemoved(ctx);
        endpoint.channel().close();
    }
}
