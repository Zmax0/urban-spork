package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.Socks5;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class ShadowsocksUDPAssociateHandler extends ChannelInboundHandlerAdapter {

    public static final ShadowsocksUDPAssociateHandler INSTANCE = new ShadowsocksUDPAssociateHandler();
    private static final Logger logger = LoggerFactory.getLogger(ShadowsocksUDPAssociateHandler.class);

    private ShadowsocksUDPAssociateHandler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Socks5CommandRequest request) {
            channelRead0(ctx, request);
        }
    }

    private void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel channel = ctx.channel();
        ServerConfig config = channel.attr(AttributeKeys.SERVER_CONFIG).get();
        if (Protocols.shadowsocks != config.getProtocol()) {
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            logger.error("UDP is not enabled");
            return;
        }
        if (request.dstAddr() == null || request.dstPort() == 0) {
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            return;
        }
        Integer bndPort = channel.attr(AttributeKeys.SOCKS_PORT).get();
        Socks5CommandRequest bndRequest = Socks5.toCommandRequest(Socks5CommandType.UDP_ASSOCIATE, new InetSocketAddress(bndPort));
        DefaultSocks5CommandResponse response = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndRequest.dstAddrType(), bndRequest.dstAddr(), bndRequest.dstPort());
        channel.writeAndFlush(response);
    }

}
