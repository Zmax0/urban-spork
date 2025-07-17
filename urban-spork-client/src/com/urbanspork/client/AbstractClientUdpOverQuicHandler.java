package com.urbanspork.client;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;
import java.time.Duration;

public abstract class AbstractClientUdpOverQuicHandler<K> extends AbstractClientUdpRelayHandler<K> {
    private final Channel endpoint;

    protected AbstractClientUdpOverQuicHandler(ClientChannelContext context, Duration keepAlive, EventLoopGroup workerGroup) {
        super(context, keepAlive);
        endpoint = ClientRelayHandler.quicEndpoint(context.config().getSsl(), workerGroup).syncUninterruptibly().channel();
    }

    protected abstract ChannelInitializer<Channel> newOutboundInitializer(K k);

    protected abstract ChannelHandler newInboundHandler(Channel inbound, K k);

    protected Channel newBindingChannel(Channel inbound, K k) {
        ServerConfig config = context.config();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        QuicChannel quicChannel = QuicChannel.newBootstrap(endpoint).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter()).connect().syncUninterruptibly().getNow();
        return quicChannel.newStreamBootstrap().handler(newOutboundInitializer(k)).create().addListener(f2 -> {
            QuicStreamChannel outbound = (QuicStreamChannel) f2.get();
            outbound.pipeline().addLast(newInboundHandler(inbound, k)); // R → L
            inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
        }).syncUninterruptibly().getNow();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        super.handlerRemoved(ctx);
        endpoint.close();
    }
}
