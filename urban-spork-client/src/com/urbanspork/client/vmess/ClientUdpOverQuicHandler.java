package com.urbanspork.client.vmess;

import com.urbanspork.client.AbstractClientUdpOverQuicHandler;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;

import java.time.Duration;

public class ClientUdpOverQuicHandler extends AbstractClientUdpOverQuicHandler<Key> implements ClientUdpOverTcp {
    public ClientUdpOverQuicHandler(ClientChannelContext context, EventLoopGroup workerGroup) {
        super(context, Duration.ofMinutes(10), workerGroup);
    }

    @Override
    public Object convertToWrite(DatagramPacketWrapper msg) {
        return ClientUdpOverTcp.super.convertToWrite(msg);
    }

    @Override
    public Key getKey(DatagramPacketWrapper msg) {
        return ClientUdpOverTcp.super.getKey(msg);
    }

    @Override
    protected ChannelInitializer<Channel> newOutboundInitializer(Key key) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ServerConfig config = context.config();
                ch.pipeline().addLast(new ClientAeadCodec(config.getCipher(), RequestCommand.UDP, key.recipient(), config.getPassword()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inbound, Key key) {
        return new ClientUdpOverTcp.InboundHandler(inbound, key.recipient(), key.sender());
    }
}
