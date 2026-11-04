package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUdpRelayHandler;
import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.logging.LoggingHandler;

class ClientUdpInitializer extends ChannelInitializer<NioDatagramChannel> {

    static final String DNS_HANDLER = "udp-dns";
    static final String RELAY_HANDLER = "udp-relay";

    private final EventLoopGroup group;
    private final ClientChannelContext context;

    ClientUdpInitializer(ClientChannelContext context, EventLoopGroup group) {
        this.group = group;
        this.context = context;
    }

    // trigger only once when bind
    @Override
    protected void initChannel(NioDatagramChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(
            new LoggingHandler(ByteBufFormat.SIMPLE),
            context.traffic(),
            new DatagramPacketEncoder(),
            new DatagramPacketDecoder()
        );
        pipeline.addLast(DNS_HANDLER, new ClientUdpDnsDecoder(context));
        pipeline.addLast(RELAY_HANDLER, newRelayHandler(group, context));
    }

    static void replace(Channel channel, EventLoopGroup group, ClientChannelContext context) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.replace(DNS_HANDLER, DNS_HANDLER, new ClientUdpDnsDecoder(context));
        pipeline.replace(RELAY_HANDLER, RELAY_HANDLER, newRelayHandler(group, context));
    }

    private static ChannelHandler newRelayHandler(EventLoopGroup workerGroup, ClientChannelContext context) {
        ServerConfig current = context.config();
        if (Protocol.vmess == current.getProtocol()) {
            if (current.quicEnabled()) {
                return new com.urbanspork.client.vmess.ClientUdpOverQuicHandler(context, workerGroup);
            }
            return new com.urbanspork.client.vmess.ClientUdpOverTcpHandler(context);
        } else if (Protocol.trojan == current.getProtocol()) {
            if (current.quicEnabled()) {
                return new com.urbanspork.client.trojan.ClientUdpOverQuicHandler(context, workerGroup);
            }
            return new com.urbanspork.client.trojan.ClientUdpOverTcpHandler(context);
        } else {
            return new ClientUdpRelayHandler(context);
        }
    }
}
