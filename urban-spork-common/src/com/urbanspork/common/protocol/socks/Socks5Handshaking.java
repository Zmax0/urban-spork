package com.urbanspork.common.protocol.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface Socks5Handshaking {

    static Promise<Result> udpAssociateNoAuth(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Result> promise = new DefaultPromise<>(executor);
        try {
            new Bootstrap().group(worker).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(
                                    new Socks5ClientEncoder(Socks5AddressEncoder.DEFAULT),
                                    new Socks5InitialResponseDecoder(),
                                    new Socks5CommandResponseDecoder(),
                                    new SimpleChannelInboundHandler<Socks5InitialResponse>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Socks5InitialResponse msg) {
                                            InetAddress inetAddress = dstAddress.getAddress();
                                            ctx.writeAndFlush(new DefaultSocks5CommandRequest(
                                                    Socks5CommandType.UDP_ASSOCIATE,
                                                    Socks5Addressing.map(inetAddress),
                                                    inetAddress.getHostAddress(),
                                                    dstAddress.getPort())
                                            );
                                        }
                                    },
                                    new SimpleChannelInboundHandler<Socks5CommandResponse>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandResponse msg) {
                                            promise.setSuccess(new Result(ch, msg));
                                            executor.shutdownGracefully();
                                        }
                                    }
                            );
                        }
                    })
                    .connect(proxyAddress).syncUninterruptibly().channel()
                    .writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH)); // greeting
        } catch (Exception e) {
            promise.setFailure(e);
        }
        return promise;
    }

    record Result(Channel sessionChannel, Socks5CommandResponse response) {}
}
