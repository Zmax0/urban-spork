package com.urbanspork.common.protocol.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

public interface ClientHandshake {
    static Promise<Result> noAuth(EventLoopGroup worker, Socks5CommandType type, InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        Promise<Result> promise = worker.next().newPromise();
        try {
            new Bootstrap().group(worker).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        Socks5InitialResponseDecoder socks5InitialResponseDecoder = new Socks5InitialResponseDecoder();
                        Socks5CommandResponseDecoder socks5CommandResponseDecoder = new Socks5CommandResponseDecoder();
                        ch.pipeline().addLast(
                            new Socks5ClientEncoder(Socks5AddressEncoder.DEFAULT),
                            socks5InitialResponseDecoder,
                            socks5CommandResponseDecoder,
                            new SimpleChannelInboundHandler<Socks5InitialResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Socks5InitialResponse msg) {
                                    ctx.writeAndFlush(Socks5.toCommandRequest(type, dstAddress));
                                    ctx.pipeline().remove(this);
                                }
                            },
                            new SimpleChannelInboundHandler<Socks5CommandResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandResponse msg) {
                                    if (msg.status().isSuccess()) {
                                        ChannelPipeline pipeline = ch.pipeline();
                                        pipeline.remove(socks5CommandResponseDecoder);
                                        pipeline.remove(socks5InitialResponseDecoder);
                                        pipeline.remove(this);
                                        promise.setSuccess(new Result(ch, msg));
                                    } else {
                                        promise.setFailure(new IllegalStateException("Unsuccessful response status: " + msg.status().toString()));
                                    }
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
