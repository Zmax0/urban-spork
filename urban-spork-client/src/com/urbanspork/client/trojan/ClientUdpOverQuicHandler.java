package com.urbanspork.client.trojan;

import com.urbanspork.client.ClientRelayHandler;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;

public class ClientUdpOverQuicHandler extends ClientUdpOverTcpHandler {
    private final Channel endpoint;

    public ClientUdpOverQuicHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, workerGroup);
        endpoint = ClientRelayHandler.quicEndpoint(config.getSsl(), workerGroup).syncUninterruptibly().channel();
    }

    @Override
    protected Channel newBindingChannel(Channel inbound, InetSocketAddress key) {
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        QuicChannel quicChannel = QuicChannel.newBootstrap(endpoint).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter() {}).connect().syncUninterruptibly().getNow();
        return quicChannel.newStreamBootstrap().handler(
            new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new ClientHeaderEncoder(config.getPassword(), serverAddress, SocksCmdType.UDP.byteValue()));
                }
            }
        ).create().addListener(f2 -> {
            QuicStreamChannel outbound = (QuicStreamChannel) f2.get();
            outbound.pipeline().addLast(newInboundHandler(inbound, key)); // R → L
            inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
        }).syncUninterruptibly().getNow();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        super.handlerRemoved(ctx);
        endpoint.close();
    }
}
