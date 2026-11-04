package com.urbanspork.client.trojan;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socks.SocksCmdType;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<InetSocketAddress> implements ClientUdpOverTcp {
    public ClientUdpOverTcpHandler(ClientChannelContext context) {
        super(context, Duration.ofMinutes(10));
    }

    @Override
    public Object convertToWrite(DatagramPacketWrapper msg) {
        return ClientUdpOverTcp.super.convertToWrite(msg);
    }

    @Override
    public InetSocketAddress getKey(DatagramPacketWrapper msg) {
        return ClientUdpOverTcp.super.getKey(msg);
    }

    @Override
    protected ChannelInitializer<Channel> newOutboundInitializer(InetSocketAddress key) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws URISyntaxException {
                context.addSslHandler(ch);
                ServerConfig config = context.config();
                addWebSocketHandler(ch);
                InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
                ch.pipeline().addLast(new ClientHeaderEncoder(config.getPassword(), serverAddress, SocksCmdType.UDP.byteValue()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inboundChannel, InetSocketAddress key) {
        return new ClientUdpOverTcp.InboundHandler(inboundChannel, key);
    }
}
