package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ShadowsocksChannelInitializer;
import com.urbanspork.client.vmess.VMessChannelInitializer;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.protocol.Protocols;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) {
        if (message instanceof Socks5CommandRequest request) {
            Channel inboundChannel = ctx.channel();
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.get();
                        if (future.isSuccess()) {
                            inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort()))
                                    .addListener((ChannelFutureListener) channelFuture -> { // socks5 command success response feature
                                        if (!ctx.isRemoved()) {
                                            ctx.pipeline().remove(ClientSocksConnectHandler.this);
                                        }
                                        outboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(inboundChannel)); // L -> R
                                        ctx.pipeline().addLast(new DefaultChannelInboundHandler(outboundChannel)); // R -> L
                                    });
                        } else {
                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                            ChannelCloseUtils.closeOnFlush(inboundChannel);
                        }
                    });
            InetSocketAddress serverAddress = inboundChannel.attr(AttributeKeys.SERVER_ADDRESS).get();
            Bootstrap bootstrap = new Bootstrap().group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            Protocols protocol = inboundChannel.attr(AttributeKeys.PROTOCOL).get();
            if (Protocols.vmess == protocol) {
                bootstrap.handler(new VMessChannelInitializer(request, inboundChannel));
            } else {
                bootstrap.handler(new ShadowsocksChannelInitializer(request, inboundChannel));
            }
            ChannelFuture connectFeature = bootstrap.connect(serverAddress);
            connectFeature.channel().pipeline().addLast(new ConnectPromiseHandler(promise));
            connectFeature.addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    ChannelCloseUtils.closeOnFlush(inboundChannel);
                }
            });
        } else {
            ctx.close();
        }
    }
}