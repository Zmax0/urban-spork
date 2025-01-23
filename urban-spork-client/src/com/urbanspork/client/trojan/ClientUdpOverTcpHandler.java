package com.urbanspork.client.trojan;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientRelayHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.socks.SocksCmdType;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<InetSocketAddress> implements ClientUdpOverTcp {
    public ClientUdpOverTcpHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10), workerGroup);
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
            protected void initChannel(Channel ch) throws SSLException, URISyntaxException {
                ClientRelayHandler.addSslHandler(ch, config);
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
