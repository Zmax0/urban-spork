package com.urbanspork.test.template;

import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TcpTestTemplate extends TestTemplate {
    final EventLoopGroup group = new NioEventLoopGroup();
    InetSocketAddress dstAddress;

    @BeforeAll
    protected void beforeAll() throws ExecutionException, InterruptedException {
        launchEchoTestServer();
    }

    private void launchEchoTestServer() throws InterruptedException, ExecutionException {
        CompletableFuture<ServerSocketChannel> promise = new CompletableFuture<>();
        POOL.submit(() -> EchoTestServer.launch(0, promise));
        dstAddress = promise.get().localAddress();
    }

    protected void socksHandshakeAndSendBytes(InetSocketAddress proxyAddress) throws ExecutionException, InterruptedException, TimeoutException {
        HandshakeResult<Socks5CommandResponse> result = com.urbanspork.common.protocol.socks.Handshake
            .noAuth(group, Socks5CommandType.CONNECT, proxyAddress, dstAddress).get();
        Channel channel = result.channel();
        sendRandomBytes(channel);
    }

    protected void httpSendBytes(InetSocketAddress proxyAddress) throws InterruptedException, ExecutionException, TimeoutException {
        httpSendBytes(proxyAddress, dstAddress);
    }

    protected void httpSendBytes(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException, TimeoutException {
        Promise<HttpRequest> promise = group.next().newPromise();
        String uri = String.format("http://%s/", NetUtil.toSocketAddressString(dstAddress));
        DefaultHttpRequest msg1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        Channel channel = new Bootstrap().group(group.next().next()).channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new HttpRequestEncoder(),
                        new HttpRequestDecoder(),
                        new SimpleChannelInboundHandler<HttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg2) {
                                msg1.setDecoderResult(msg2.decoderResult());
                                if (msg1.equals(msg2)) {
                                    promise.setSuccess(msg2);
                                } else {
                                    promise.setFailure(new RuntimeException("Received unexpected msg"));
                                }
                            }
                        }
                    );
                }
            })
            .connect(proxyAddress).sync().channel();
        channel.writeAndFlush(msg1);
        channel.closeFuture().addListener(future -> {
            if (!promise.isDone()) {
                promise.setFailure(new IllegalStateException("Channel closed"));
            }
        });
        promise.get(10, TimeUnit.SECONDS);
    }

    protected void httpsHandshakeAndSendBytes(InetSocketAddress proxyAddress) throws ExecutionException, InterruptedException, TimeoutException {
        HandshakeResult<HttpResponse> result = com.urbanspork.common.protocol.https.proxy.Handshake
            .start(group, proxyAddress, dstAddress).get();
        Channel channel = result.channel();
        sendRandomBytes(channel);
    }

    private static void sendRandomBytes(Channel channel) throws InterruptedException, ExecutionException, TimeoutException {
        int length = ThreadLocalRandom.current().nextInt(0xfff, 0xffff);
        byte[] bytes = Dice.rollBytes(length);
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(
            new FixedLengthFrameDecoder(length),
            new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                    if (Arrays.equals(ByteBufUtil.getBytes(msg), bytes)) {
                        promise.setSuccess(null);
                    } else {
                        promise.setFailure(new RuntimeException("Received unexpected msg"));
                    }
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.setFailure(cause);
                }
            }
        );
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    protected void afterAll() {
        group.shutdownGracefully();
    }
}
