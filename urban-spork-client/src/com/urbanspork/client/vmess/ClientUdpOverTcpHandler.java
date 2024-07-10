package com.urbanspork.client.vmess;

import com.urbanspork.client.AbstractClientUdpOverTcpHandler;
import com.urbanspork.client.ClientInitializer;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

public class ClientUdpOverTcpHandler extends AbstractClientUdpOverTcpHandler<ClientUdpOverTcpHandler.Key> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUdpOverTcpHandler.class);

    public ClientUdpOverTcpHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10), workerGroup);
    }

    @Override
    protected Object convertToWrite(DatagramPacketWrapper msg) {
        return msg.packet().content();
    }

    @Override
    protected Key getKey(DatagramPacketWrapper msg) {
        return new Key(msg.packet().sender(), msg.proxy() /* recipient */);
    }

    @Override
    protected ChannelInitializer<Channel> newOutboundInitializer(Key key) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws URISyntaxException {
                ChannelPipeline pipeline = ch.pipeline();
                if (config.wsEnabled()) {
                    pipeline.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(0xffff),
                        ClientInitializer.buildWebSocketHandler(config),
                        new WebSocketCodec()
                    );
                }
                pipeline.addLast(new ClientAeadCodec(config.getCipher(), RequestCommand.UDP, key.recipient, config.getPassword()));
            }
        };
    }

    @Override
    protected ChannelHandler newInboundHandler(Channel inboundChannel, Key key) {
        return new InboundHandler(inboundChannel, key.recipient, key.sender);
    }

    public record Key(InetSocketAddress sender, InetSocketAddress recipient) {
        @Override
        public String toString() {
            return "[" + sender + " - " + recipient + "]";
        }
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final Channel channel;
        private final InetSocketAddress sender;
        private final InetSocketAddress recipient;

        InboundHandler(Channel channel, InetSocketAddress recipient, InetSocketAddress sender) {
            super(false);
            this.channel = channel;
            this.recipient = recipient;
            this.sender = sender;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            Channel inboundChannel = ctx.channel();
            logger.info("[udp][vmess]{} ← {} ~ {} ← {}", sender, inboundChannel.localAddress(), inboundChannel.remoteAddress(), recipient);
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(msg, recipient), sender));
        }
    }

    private static class WebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private ChannelPromise promise;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            promise = ctx.newPromise();
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(msg.retain());
            if (!promise.isDone()) {
                promise.addListener(f -> ctx.writeAndFlush(frame));
            } else {
                out.add(frame);
            }
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {
            out.add(msg.retain().content());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                promise.setSuccess();
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}
