package com.urbanspork.client.trojan;

import com.urbanspork.client.AbstractClientUdpOverQuicHandler;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.socks.SocksCmdType;

import java.net.InetSocketAddress;
import java.time.Duration;

public class ClientUdpOverQuicHandler extends AbstractClientUdpOverQuicHandler<InetSocketAddress> implements ClientUdpOverTcp {
    public ClientUdpOverQuicHandler(ClientChannelContext context, EventLoopGroup workerGroup) {
        super(context, Duration.ofMinutes(10), workerGroup);
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
    protected ChannelInitializer<Channel> newOutboundInitializer(InetSocketAddress ignore) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ServerConfig config = context.config();
                InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
                ch.pipeline().addLast(new ClientHeaderEncoder(config.getPassword(), serverAddress, SocksCmdType.UDP.byteValue()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inbound, InetSocketAddress address) {
        return new ClientUdpOverTcp.InboundHandler(inbound, address);
    }
}
