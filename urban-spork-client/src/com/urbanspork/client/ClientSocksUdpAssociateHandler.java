package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
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
public class ClientSocksUdpAssociateHandler extends ChannelInboundHandlerAdapter {

    public static final ClientSocksUdpAssociateHandler INSTANCE = new ClientSocksUdpAssociateHandler();
    private static final Logger logger = LoggerFactory.getLogger(ClientSocksUdpAssociateHandler.class);

    private ClientSocksUdpAssociateHandler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        channelRead0(ctx, (Socks5CommandRequest) msg);
    }

    private void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel channel = ctx.channel();
        ServerConfig config = channel.attr(ClientChannelContext.KEY).get().config();
        if (Protocol.vmess == config.getProtocol() && !config.udpEnabled()) {
            logger.error("UDP is not enabled");
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            return;
        }
        if (request.dstPort() == 0) {
            logger.error("Illegal destination address");
            channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            return;
        }
        Socks5CommandRequest bndRequest = Socks5.toCommandRequest(Socks5CommandType.UDP_ASSOCIATE, (InetSocketAddress) channel.localAddress());
        DefaultSocks5CommandResponse response = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndRequest.dstAddrType(), bndRequest.dstAddr(), bndRequest.dstPort());
        channel.writeAndFlush(response);
    }
}
