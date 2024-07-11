package com.urbanspork.client.trojan;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientTcpRelayHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socks.SocksCmdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<InetSocketAddress> {
    private static final Logger logger = LoggerFactory.getLogger(ClientUdpOverTcpHandler.class);

    public ClientUdpOverTcpHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10), workerGroup);
    }

    @Override
    protected Object convertToWrite(DatagramPacketWrapper msg) {
        DatagramPacket packet = msg.packet();
        ByteBuf buffer = Unpooled.buffer();
        Address.encode(msg.proxy(), buffer);
        ByteBuf content = packet.content();
        buffer.writeShort(content.readableBytes());
        buffer.writeBytes(Trojan.CRLF);
        buffer.writeBytes(content);
        return buffer;
    }

    @Override
    protected InetSocketAddress getKey(DatagramPacketWrapper msg) {
        return msg.packet().sender();
    }

    @Override
    protected ChannelInitializer<Channel> newOutboundInitializer(InetSocketAddress key) {
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws SSLException {
                ch.pipeline().addLast(
                    ClientTcpRelayHandler.buildSslHandler(ch, config),
                    new ClientHeaderEncoder(config.getPassword(), serverAddress, SocksCmdType.UDP.byteValue())
                );
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inboundChannel, InetSocketAddress key) {
        return new InboundHandler(inboundChannel, key);
    }

    private static class InboundHandler extends ByteToMessageDecoder {
        private final Channel channel;
        private final InetSocketAddress sender;

        InboundHandler(Channel channel, InetSocketAddress sender) {
            this.channel = channel;
            this.sender = sender;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            Channel outboundChannel = ctx.channel();
            InetSocketAddress address = Address.decode(in);
            logger.info("[udp][trojan]{}←{}~{}←{}", sender, address, channel.localAddress(), outboundChannel.localAddress());
            short length = in.readShort();
            in.skipBytes(Trojan.CRLF.length);
            ByteBuf content = in.readBytes(length);
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(content, address), sender));
        }
    }
}
