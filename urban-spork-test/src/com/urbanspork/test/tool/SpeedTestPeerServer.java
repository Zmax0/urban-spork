package com.urbanspork.test.tool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class SpeedTestPeerServer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SpeedTestPeerServer.class);
    private static final int FILLER_BYTES = 64 * 1024;
    private static final ByteBuf FILLER = filler();

    public static final int PORT = 16901;
    public static final long DEFAULT_PAYLOAD = 256L * 1024 * 1024;

    private final EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    private final EventLoopGroup worker = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
    private Channel channel;

    private SpeedTestPeerServer() {
    }

    public static SpeedTestPeerServer launch(String host, int port) throws InterruptedException {
        SpeedTestPeerServer server = new SpeedTestPeerServer();
        server.channel = new ServerBootstrap().group(server.boss, server.worker)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(
                        new HttpServerCodec(),
                        new HttpObjectAggregator(64 * 1024),
                        new DownloadHandler()
                    );
                }
            })
            .bind(host, port).sync().channel();
        logger.info("peer listening on {}", server.localAddress());
        return server;
    }

    static void main() throws InterruptedException {
        int port = Integer.parseInt(System.getProperty("dstPort", String.valueOf(PORT)));
        String host = System.getProperty("dstHost", InetAddress.getLoopbackAddress().getHostName());
        try (SpeedTestPeerServer server = launch(host, port)) {
            server.channel.closeFuture().sync();
        }
    }

    public InetSocketAddress localAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public void close() {
        channel.close().syncUninterruptibly();
        worker.shutdownGracefully().syncUninterruptibly();
        boss.shutdownGracefully().syncUninterruptibly();
    }

    private static final class DownloadHandler extends ChannelInboundHandlerAdapter {
        private long remaining;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (!(msg instanceof HttpRequest request) || !HttpMethod.GET.equals(request.method())) {
                    ctx.close();
                    return;
                }
                ctx.channel().config().setAutoRead(false);
                remaining = payloadOf(request.uri());
                DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, remaining);
                response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-store");
                ctx.write(response);
                pump(ctx);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        private void pump(ChannelHandlerContext ctx) {
            while (remaining > 0 && ctx.channel().isWritable()) {
                int chunk = (int) Math.min(FILLER.readableBytes(), remaining);
                remaining -= chunk;
                ctx.write(FILLER.retainedSlice(FILLER.readerIndex(), chunk));
            }
            if (remaining > 0) {
                ctx.flush();
                return;
            }
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            if (ctx.channel().isWritable() && remaining > 0) {
                pump(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("peer connection failed: {}", cause.toString(), cause);
            ctx.close();
        }

        private static long payloadOf(String uri) {
            int slash = uri == null ? -1 : uri.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < uri.length()) {
                try {
                    return Long.parseLong(uri.substring(slash + 1));
                } catch (NumberFormatException _) {
                    // use default
                }
            }
            return DEFAULT_PAYLOAD;
        }
    }

    private static ByteBuf filler() {
        byte[] pattern = new byte[FILLER_BYTES];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = (byte) (i * 31);
        }
        return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(pattern));
    }
}
