package com.urbanspork.client.vmess;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientRelayHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<ClientUdpOverTcpHandler.Key> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUdpOverTcpHandler.class);

    public ClientUdpOverTcpHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10), workerGroup);
    }

    @Override
    protected Object convertToWrite(DatagramPacketWrapper msg) {
        return msg.packet().content();
    }

    @Override
    protected Key getKey(DatagramPacketWrapper msg) {
        return new Key(msg.packet().sender(), msg.proxy() /* recipient */);
    }

    @Override
    protected ChannelInitializer<Channel> newOutboundInitializer(Key key) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws URISyntaxException, SSLException {
                ClientRelayHandler.addSslHandler(ch, config);
                addWebSocketHandler(ch);
                ch.pipeline().addLast(new ClientAeadCodec(config.getCipher(), RequestCommand.UDP, key.recipient, config.getPassword()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inboundChannel, Key key) {
        return new InboundHandler(inboundChannel, key.recipient, key.sender);
    }

    public record Key(InetSocketAddress sender, InetSocketAddress recipient) {
        @Override
        public String toString() {
            return "[" + sender + " - " + recipient + "]";
        }
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final Channel channel;
        private final InetSocketAddress sender;
        private final InetSocketAddress recipient;

        InboundHandler(Channel channel, InetSocketAddress recipient, InetSocketAddress sender) {
            super(false);
            this.channel = channel;
            this.recipient = recipient;
            this.sender = sender;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            Channel inboundChannel = ctx.channel();
            logger.info("[udp][vmess]{} ← {} ~ {} ← {}", sender, inboundChannel.localAddress(), inboundChannel.remoteAddress(), recipient);
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(msg, recipient), sender));
        }
    }
}
