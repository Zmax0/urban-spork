package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.socks.udp.Socks5DatagramPacketDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientUDPReplayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ClientUDPReplayHandler.class);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private static final Map<InetSocketAddress, Channel> binding = new ConcurrentHashMap<>();
    private final ServerConfig config;

    public ClientUDPReplayHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Socks5DatagramPacketDecoder.Result result) {
            InetSocketAddress dstAddress = result.dstAddr();
            ctx.channel().attr(AttributeKeys.SOCKS5_DST_ADDR).set(dstAddress);
            DatagramPacket packet = result.data();
            InetSocketAddress sender = packet.sender();
            Channel bndChannel = binding.computeIfAbsent(sender, key -> bind(ctx.channel(), sender));
            bndChannel.writeAndFlush(new DatagramPacket(packet.content(), dstAddress));
        }
    }

    private Channel bind(Channel inboundChannel, InetSocketAddress sender) {
        Channel channel = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.attr(AttributeKeys.REPLAY_ADDRESS).set(new InetSocketAddress(config.getHost(), config.getPort()));
                        ch.pipeline().addLast(
                                new ShadowsocksUDPReplayCodec(ShadowsocksAEADCipherCodecs.get(config.getPassword(), config.getCipher(), Network.UDP)),
                                new InboundHandler(inboundChannel, sender)// server->client->sender
                        );
                    }
                })
                .bind(0) // automatically assigned port now, may have security implications
                .syncUninterruptibly().channel();
        logger.info("New binding => {} - {}", sender, channel);
        return channel;
    }

    private static class InboundHandler extends DefaultChannelInboundHandler {

        private final InetSocketAddress sender;

        InboundHandler(Channel channel, InetSocketAddress sender) {
            super(channel);
            this.sender = sender;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof DatagramPacket packet) {
                msg = new DatagramPacket(packet.content(), sender);
                super.channelRead(ctx, msg);
            }
        }
    }

}
