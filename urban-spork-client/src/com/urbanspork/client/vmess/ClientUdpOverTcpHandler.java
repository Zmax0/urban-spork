package com.urbanspork.client.vmess;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.client.ClientRelayHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;

import javax.net.ssl.SSLException;
import java.net.URISyntaxException;
import java.time.Duration;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<Key> implements ClientUdpOverTcp {

    public ClientUdpOverTcpHandler(ClientChannelContext context, EventLoopGroup workerGroup) {
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
            protected void initChannel(Channel ch) throws URISyntaxException, SSLException {
                ClientRelayHandler.addSslHandler(ch, context);
                addWebSocketHandler(ch);
                ServerConfig config = context.config();
                ch.pipeline().addLast(new ClientAeadCodec(config.getCipher(), RequestCommand.UDP, key.recipient(), config.getPassword()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inboundChannel, Key key) {
        return new ClientUdpOverTcp.InboundHandler(inboundChannel, key.recipient(), key.sender());
    }
}
