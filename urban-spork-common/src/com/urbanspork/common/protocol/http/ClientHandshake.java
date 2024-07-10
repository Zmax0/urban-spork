package com.urbanspork.common.protocol.http;

import com.urbanspork.common.protocol.HandshakeResult;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

public interface ClientHandshake {
    static Promise<HandshakeResult<HttpResponse>> https(EventLoopGroup worker, InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        Promise<HandshakeResult<HttpResponse>> promise = worker.next().newPromise();
        try {
            new Bootstrap().group(worker).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        StringEncoder stringEncoder = new StringEncoder();
                        HttpResponseDecoder httpResponseDecoder = new HttpResponseDecoder();
                        ch.pipeline().addLast(
                            stringEncoder,
                            httpResponseDecoder,
                            new SimpleChannelInboundHandler<HttpResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) {
                                    if (msg.status().code() == HttpResponseStatus.OK.code()) {
                                        ChannelPipeline pipeline = ch.pipeline();
                                        pipeline.remove(stringEncoder);
                                        pipeline.remove(httpResponseDecoder);
                                        pipeline.remove(this);
                                        promise.setSuccess(new HandshakeResult<>(ch, msg));
                                    } else {
                                        promise.setFailure(new IllegalStateException("Unsuccessful response status: " + msg.status()));
                                    }
                                }
                            }
                        );
                    }
                })
                .connect(proxyAddress).syncUninterruptibly().channel()
                .writeAndFlush(HttpMethod.CONNECT.asciiName() + " " + dstAddress.getHostName() + ":" + dstAddress.getPort() + " HTTP/1.1\r\nConnection: keep-alive\r\n\r\n"); // greeting
        } catch (Exception e) {
            promise.setFailure(e);
        }
        return promise;
    }
}
