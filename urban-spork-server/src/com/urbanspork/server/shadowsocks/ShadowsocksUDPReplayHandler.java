package com.urbanspork.server.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class ShadowsocksUDPReplayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ShadowsocksUDPReplayHandler.class);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel outboundChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel inboundChannel = ctx.channel();
        outboundChannel = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
                .handler(new DefaultChannelInboundHandler(inboundChannel) {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof DatagramPacket packet) {
                            logger.info("Replay datagram packet [L: {} <- R: {}]", inboundChannel.localAddress(), packet.sender());
                        }
                        super.channelRead(ctx, msg);
                    }
                })// sender->server->client
                .bind(0) // automatically assigned port now, may have security implications
                .syncUninterruptibly().channel();
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket packet) {
            InetSocketAddress sender = packet.sender();
            Channel channel = ctx.channel();
            channel.attr(AttributeKeys.REPLAY_ADDRESS).set(sender);
            logger.info("Replay datagram packet [L: {} -> R: {}]", channel.localAddress(), packet.recipient());
            outboundChannel.writeAndFlush(packet);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel.isActive()) {
            outboundChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (outboundChannel.isActive()) {
            outboundChannel.close();
        }
        ctx.close();
    }
}