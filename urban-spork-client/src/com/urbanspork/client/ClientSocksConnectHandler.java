package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.protocol.ShadowsocksProtocolEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
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
        Channel localChannel = ctx.channel();
        InetSocketAddress serverAddress = localChannel.attr(AttributeKeys.SERVER_ADDRESS).get();
        if (message instanceof Socks5CommandRequest request) {
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ChannelFuture responseFuture = localChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort()));
                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                ctx.pipeline().remove(ClientSocksConnectHandler.this);
                                outboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(localChannel));
                                ctx.pipeline().addLast(new DefaultChannelInboundHandler(outboundChannel));
                            });
                        } else {
                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                            ChannelCloseUtils.closeOnFlush(localChannel);
                        }
                    });
            b.group(localChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel remoteChannel) {
                            remoteChannel.attr(AttributeKeys.REQUEST).set(request);
                            remoteChannel.pipeline()
                                    .addLast(new ShadowsocksCipherCodec(localChannel.attr(AttributeKeys.CIPHER).get(), localChannel.attr(AttributeKeys.KEY).get()))
                                    .addLast(new ShadowsocksProtocolEncoder())
                                    .addLast(new ClientPromiseHandler(promise));
                        }
                    }).connect(serverAddress).addListener((ChannelFutureListener) future -> {
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
