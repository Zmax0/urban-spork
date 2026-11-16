package com.urbanspork.test.interop;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.client.Socks5Handshake;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class InteropTraffic {
    private InteropTraffic() {}

    static void tcp(InetSocketAddress proxy, InetSocketAddress target) throws Exception {
        try (var writer = Executors.newVirtualThreadPerTaskExecutor(); Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, proxy))) {
            socket.connect(target, 5000);
            socket.setSoTimeout(10000);
            for (int size : new int[]{64, 1024 * 1024}) {
                byte[] expected = payload(size, 0);
                var written = writer.submit(() -> {
                    try {
                        socket.getOutputStream().write(expected);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                byte[] actual = socket.getInputStream().readNBytes(size);
                if (actual.length != size) {
                    throw new IOException("TCP closed after " + actual.length + " of " + size + " bytes");
                }
                Assertions.assertArrayEquals(expected, actual, "TCP payload size=" + size);
                written.get(10, TimeUnit.SECONDS);
            }
        }
    }

    static void udp(EventLoopGroup group, InetSocketAddress proxy, InetSocketAddress target, int[] payloadSizes) throws Exception {
        LinkedBlockingQueue<Received> received = new LinkedBlockingQueue<>();
        Channel channel = new Bootstrap().group(group).channel(NioDatagramChannel.class)
            .option(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new DatagramPacketEncoder(), new DatagramPacketDecoder(), new SimpleChannelInboundHandler<DatagramPacketWrapper>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper packet) {
                            received.add(new Received(packet.server(), ByteBufUtil.getBytes(packet.packet().content())));
                        }
                    });
                }
            }).bind("127.0.0.1", 0).sync().channel();
        try {
            var association = Socks5Handshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxy, (InetSocketAddress) channel.localAddress()).get(10, TimeUnit.SECONDS);
            try {
                var response = association.response();
                Assertions.assertEquals(Socks5CommandStatus.SUCCESS, response.status());
                InetSocketAddress relay = new InetSocketAddress(response.bndAddr(), response.bndPort());
                int[] sizes = Arrays.copyOf(payloadSizes, payloadSizes.length + 8);
                Arrays.fill(sizes, payloadSizes.length, sizes.length, 128);
                // Send a burst before reading to exercise packet boundaries on stream transports.
                for (int sequence = 0; sequence < sizes.length; sequence++) {
                    byte[] expected = payload(sizes[sequence], sequence);
                    channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(Unpooled.wrappedBuffer(expected), target), relay)).sync();
                }
                for (int sequence = 0; sequence < sizes.length; sequence++) {
                    byte[] expected = payload(sizes[sequence], sequence);
                    Received actual = received.poll(10, TimeUnit.SECONDS);
                    Assertions.assertNotNull(actual, "UDP response timeout, sequence=" + sequence + ", size=" + expected.length);
                    Assertions.assertEquals(target, actual.target());
                    Assertions.assertArrayEquals(expected, actual.payload(), "UDP sequence=" + sequence);
                }
            } finally {
                association.channel().close().syncUninterruptibly();
            }
        } finally {
            channel.close().syncUninterruptibly();
        }
    }

    private static byte[] payload(int length, int sequence) {
        byte[] bytes = new byte[length];
        new Random(0x5eedL + length + sequence).nextBytes(bytes);
        return bytes;
    }

    private record Received(InetSocketAddress target, byte[] payload) {}
}
