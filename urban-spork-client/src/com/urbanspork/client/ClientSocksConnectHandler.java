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

@ChannelHandler.Sharable
public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) {
        if (message instanceof Socks5CommandRequest request) {
            Channel localChannel = ctx.channel();
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.get();
                        if (future.isSuccess()) {
                            ChannelFuture responseFuture = localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort()));
                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                if (!ctx.isRemoved()) {
                                    ctx.pipeline().remove(ClientSocksConnectHandler.this);
                                }
                                outboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(localChannel));
                                ctx.pipeline().addLast(new DefaultChannelInboundHandler(outboundChannel));
                            });
                        } else {
                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                            ChannelCloseUtils.closeOnFlush(localChannel);
                        }
                    });
            InetSocketAddress serverAddress = localChannel.attr(AttributeKeys.SERVER_ADDRESS).get();
            Bootstrap bootstrap = b.group(localChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true);
            Protocols protocol = localChannel.attr(AttributeKeys.PROTOCOL).get();
            if (Protocols.vmess == protocol) {
                bootstrap.handler(new VMessChannelInitializer(request, localChannel, promise));
            } else {
                bootstrap.handler(new ShadowsocksChannelInitializer(request, localChannel, promise));
            }
            bootstrap.connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    ChannelCloseUtils.closeOnFlush(localChannel);
                }
            });
        } else {
            ctx.close();
        }
    }
}