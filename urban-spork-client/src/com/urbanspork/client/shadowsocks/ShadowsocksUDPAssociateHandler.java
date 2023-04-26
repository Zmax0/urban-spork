package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.socks.Socks5Addressing;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class ShadowsocksUDPAssociateHandler extends ChannelInboundHandlerAdapter {

    public static final ShadowsocksUDPAssociateHandler INSTANCE = new ShadowsocksUDPAssociateHandler();
    private static final Logger logger = LoggerFactory.getLogger(ShadowsocksUDPAssociateHandler.class);

    private ShadowsocksUDPAssociateHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Socks5CommandRequest request) {
            channelRead0(ctx, request);
        }
    }

    private void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel channel = ctx.channel();
        ServerConfig config = channel.attr(AttributeKeys.SERVER_CONFIG).get();
        if (!config.udpEnabled()) {
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            logger.error("UDP is not enabled");
            return;
        }
        if (request.dstAddr() == null || request.dstPort() == 0) {
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            return;
        }
        Integer bndPort = channel.attr(AttributeKeys.SOCKS_PORT).get();
        InetSocketAddress address = new InetSocketAddress(bndPort);
        InetAddress inetAddress = address.getAddress();
        Socks5AddressType bndAddrType = Socks5Addressing.map(inetAddress);
        String bndAddr = inetAddress.getHostAddress();
        DefaultSocks5CommandResponse response = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndAddrType, bndAddr, bndPort);
        channel.writeAndFlush(response);
    }

}
