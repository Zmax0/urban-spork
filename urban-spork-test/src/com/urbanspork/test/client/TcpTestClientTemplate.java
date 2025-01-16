package com.urbanspork.test.client;

import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.test.server.tcp.HttpTestServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

abstract class TcpTestClientTemplate<T> extends TestClientTemplate {
    protected static final Logger logger = LoggerFactory.getLogger(TcpTestClientTemplate.class);
    protected Channel channel;
    protected final EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    protected void launch() throws IOException, ExecutionException, InterruptedException {
        InetSocketAddress proxyAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), proxyPort);
        InetSocketAddress dstAddress = new InetSocketAddress(this.dstAddress, HttpTestServer.PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            connect(proxyAddress, dstAddress);
            byte[] bytes = line.getBytes();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
            logger.info("Send msg");
        }
        bossGroup.shutdownGracefully();
    }

    private void connect(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        if (channel == null) {
            HandshakeResult<T> result = handshake(proxyAddress, dstAddress);
            channel = result.channel();
            logger.info("Launch {} => {}", this.getClass().getSimpleName(), channel.localAddress());
            channel.pipeline().addLast(
                new LoggingHandler(LogLevel.TRACE),
                new StringDecoder(),
                new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                        logger.info("↓ Receive msg ↓\n{}", msg);
                    }

                    @Override
                    public void channelUnregistered(ChannelHandlerContext ctx) {
                        logger.info("Channel unregistered");
                        channel = null;
                    }
                }
            );
        }
    }

    protected abstract HandshakeResult<T> handshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException;
}
