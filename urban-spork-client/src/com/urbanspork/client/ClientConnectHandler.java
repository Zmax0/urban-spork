package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.socks.SocksCmdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

interface ClientConnectHandler {
    Logger logger = LoggerFactory.getLogger(ClientConnectHandler.class);

    ChannelHandler inboundHandler();

    InboundWriter inboundWriter();

    Consumer<Channel> outboundWriter();

    default void connect(Channel inbound, InetSocketAddress dstAddress) {
        ServerConfig config = inbound.attr(AttributeKeys.SERVER_CONFIG).get();
        boolean autoResponse = !config.wsEnabled();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        new Bootstrap()
            .group(inbound.eventLoop())
            .channel(inbound.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(getOutboundChannelHandler(inbound, dstAddress, config))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
                    inbound.pipeline().remove(inboundHandler());
                    if (autoResponse) {
                        inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound));
                        inboundWriter().success().accept(inbound);
                        outboundWriter().accept(outbound);
                    }
                } else {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    inboundWriter().failure().accept(inbound);
                    ChannelCloseUtils.closeOnFlush(inbound);
                }
            });
    }

    private ChannelHandler getOutboundChannelHandler(Channel inbound, InetSocketAddress address, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                if (config.wsEnabled()) {
                    enableWebSocket(inbound, outbound, config);
                }
                switch (config.getProtocol()) {
                    case vmess -> outbound.pipeline().addLast(new ClientAeadCodec(config.getCipher(), address, config.getPassword()));
                    case trojan -> outbound.pipeline().addLast(
                        ClientInitializer.buildSslHandler(outbound, config),
                        new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue())
                    );
                    default -> outbound.pipeline().addLast(new TcpRelayCodec(new Context(), config, address, Mode.Client));
                }
            }
        };
    }

    private void enableWebSocket(Channel inbound, Channel outbound, ServerConfig config) throws URISyntaxException {
        outbound.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(0xffff),
            ClientInitializer.buildWebSocketHandler(config),
            new WebSocketCodec(inbound, config, inboundWriter(), outboundWriter())
        );
        inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
    }

    record InboundWriter(Consumer<Channel> success, Consumer<Channel> failure) {}

    class WebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final Channel inbound;
        private final ServerConfig config;
        private final InboundWriter inboundWriter;
        private final Consumer<Channel> outboundWriter;

        public WebSocketCodec(Channel inbound, ServerConfig config, InboundWriter inboundWriter, Consumer<Channel> outboundWriter) {
            this.inbound = inbound;
            this.config = config;
            this.inboundWriter = inboundWriter;
            this.outboundWriter = outboundWriter;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
            out.add(new BinaryWebSocketFrame(msg.retain()));
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {
            out.add(msg.retain().content());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                inbound.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx2, Object msg) {
                        ctx.channel().writeAndFlush(msg);
                    }
                }); // L → R
                inboundWriter.success().accept(inbound);
                outboundWriter.accept(ctx.channel());
            }
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                logger.error("Connect proxy websocket server {}:{} time out", config.getHost(), config.getPort());
                inboundWriter.failure().accept(inbound);
                ChannelCloseUtils.closeOnFlush(inbound);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}
