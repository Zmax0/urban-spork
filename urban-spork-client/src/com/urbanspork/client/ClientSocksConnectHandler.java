package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientTCPChannelInitializer;
import com.urbanspork.client.vmess.ClientChannelInitializer;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel inboundChannel = ctx.channel();
        ServerConfig config = inboundChannel.attr(AttributeKeys.SERVER_CONFIG).get();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        ChannelHandler outboundHandler;
        if (Protocols.vmess == config.getProtocol()) {
            outboundHandler = new ClientChannelInitializer(request, config);
        } else {
            outboundHandler = new ClientTCPChannelInitializer(request, config);
        }
        new Bootstrap()
            .group(inboundChannel.eventLoop())
            .channel(inboundChannel.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(outboundHandler)
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new DefaultChannelInboundHandler(ctx.channel())); // R → L
                    ctx.pipeline().remove(ClientSocksConnectHandler.this);
                    InetSocketAddress localAddress = (InetSocketAddress) inboundChannel.localAddress();
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), localAddress.getHostString(), localAddress.getPort()))
                        .addListener((ChannelFutureListener) channelFuture -> ctx.pipeline().addLast(new DefaultChannelInboundHandler(outbound))); // L → R
                } else {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    Channel inbound = ctx.channel();
                    inbound.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    ChannelCloseUtils.closeOnFlush(inbound);
                }
            });
    }
}