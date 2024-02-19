package com.urbanspork.client.vmess;

import com.urbanspork.client.AbstractClientUdpRelayHandler;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.socks.Socks5;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

public class ClientUdpOverTCPHandler extends AbstractClientUdpRelayHandler<ClientUdpOverTCPHandler.Key> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUdpOverTCPHandler.class);
    private final EventLoopGroup workerGroup;

    public ClientUdpOverTCPHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10));
        this.workerGroup = workerGroup;
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
    protected Channel newBindingChannel(Channel inboundChannel, Key key) {
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        return new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    Socks5CommandRequest request = Socks5.toCommandRequest(Socks5CommandType.CONNECT, key.recipient);
                    ch.pipeline().addLast(new ClientAEADCodec(config.getCipher(), RequestCommand.UDP, request, config.getPassword()));
                }
            })
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new InboundHandler(inboundChannel, key.recipient, key.sender)); // R → L
                    inboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                } else {
                    logger.error("Connect relay server {} failed", serverAddress);
                }
            }).syncUninterruptibly().channel();
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
