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
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface ClientTcpRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientTcpRelayHandler.class);

    default Consumer<Channel> outboundReady(Channel inbound) {
        return channel -> {};
    }

    default InboundReady inboundReady() {
        return new InboundReady(channel -> {}, channel -> {});
    }

    default void connect(Channel inbound, InetSocketAddress dstAddress) {
        ServerConfig config = inbound.attr(AttributeKeys.SERVER_CONFIG).get();
        if (config.quicEnabled()) {
            quic(inbound, dstAddress, config);
        } else {
            tcp(inbound, dstAddress, config);
        }
    }

    private void tcp(Channel inbound, InetSocketAddress dst, ServerConfig config) {
        boolean isReadyOnceConnected = config.getWs() == null;
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        new Bootstrap()
            .group(inbound.eventLoop())
            .channel(inbound.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(getOutboundChannelHandler(inbound, dst, config))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
                    if (isReadyOnceConnected) {
                        inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                        inboundReady().success().accept(inbound);
                        outboundReady(inbound).accept(outbound);
                    }
                } else {
                    logger.error("connect proxy server {} failed", serverAddress, future.cause());
                    handleFailure(inbound);
                }
            });
    }

    private void quic(Channel inbound, InetSocketAddress dst, ServerConfig config) {
        SslSetting sslSetting = config.getSsl();
        QuicSslContext context = QuicSslContextBuilder.forClient().trustManager(new File(sslSetting.getCertificateFile())).applicationProtocols().applicationProtocols("http/1.1").build();
        ChannelHandler codec = new QuicClientCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(0xfffff)
            .initialMaxStreamDataBidirectionalLocal(0xfffff)
            .build();
        new Bootstrap().group(inbound.eventLoop()).channel(NioDatagramChannel.class).handler(codec).bind(0).addListener((ChannelFutureListener) f0 -> {
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel ch0 = f0.channel();
            QuicChannel.newBootstrap(ch0).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter() {}).connect().addListener(f1 -> {
                    if (f1.isSuccess()) {
                        QuicChannel quicChannel = (QuicChannel) f1.get();
                        quicChannel.newStreamBootstrap().handler(
                            new ChannelInitializer<>() {
                                @Override
                                protected void initChannel(Channel ch) {
                                    addOutboundProtocolHandler(dst, config, ch);
                                }
                            }
                        ).create().addListener(f2 -> {
                            if (f2.isSuccess()) {
                                QuicStreamChannel outbound = (QuicStreamChannel) f2.get();
                                outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
                                inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                                inboundReady().success().accept(inbound);
                                outboundReady(inbound).accept(outbound);
                            } else {
                                logger.error("create quic stream failed", f2.cause());
                                quicChannel.close();
                                handleFailure(inbound);
                            }
                        });
                    } else {
                        logger.error("connect proxy server {} failed", serverAddress, f1.cause());
                        ch0.close();
                        handleFailure(inbound);
                    }
                }
            );
        });
    }

    private ChannelHandler getOutboundChannelHandler(Channel inbound, InetSocketAddress address, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                addSslHandler(outbound, config);
                if (addWebSocketHandlers(outbound, config)) {
                    outbound.pipeline().addLast(new WebSocketCodec(inbound, config, ClientTcpRelayHandler.this));
                    inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
                }
                addOutboundProtocolHandler(address, config, outbound);
            }
        };
    }

    private static void addOutboundProtocolHandler(InetSocketAddress dstAddress, ServerConfig config, Channel outbound) {
        switch (config.getProtocol()) {
            case vmess -> outbound.pipeline().addLast(new ClientAeadCodec(config.getCipher(), dstAddress, config.getPassword()));
            case trojan -> outbound.pipeline().addLast(new ClientHeaderEncoder(config.getPassword(), dstAddress, SocksCmdType.CONNECT.byteValue()));
            default -> outbound.pipeline().addLast(new TcpRelayCodec(new Context(), config, dstAddress, Mode.Client, ServerUserManager.empty()));
        }
    }

    private void handleFailure(Channel inbound) {
        inboundReady().failure().accept(inbound);
        ChannelCloseUtils.closeOnFlush(inbound);
    }

    record InboundReady(Consumer<Channel> success, Consumer<Channel> failure) {}

    class WebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final Channel inbound;
        private final ServerConfig config;
        private final ClientTcpRelayHandler relayHandler;

        public WebSocketCodec(Channel inbound, ServerConfig config, ClientTcpRelayHandler relayHandler) {
            this.inbound = inbound;
            this.config = config;
            this.relayHandler = relayHandler;
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
                relayHandler.inboundReady().success().accept(inbound);
                relayHandler.outboundReady(inbound).accept(ctx.channel());
            }
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                logger.error("Connect proxy websocket server {}:{} time out", config.getHost(), config.getPort());
                relayHandler.inboundReady().failure().accept(inbound);
                ChannelCloseUtils.closeOnFlush(inbound);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

    static void addSslHandler(Channel ch, ServerConfig config) throws SSLException {
        SslSetting sslSetting = config.getSsl();
        if (sslSetting == null) {
            if (Protocol.trojan == config.getProtocol()) {
                throw new IllegalArgumentException("required security setting not present");
            } else {
                return;
            }
        }
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        String serverName = config.getHost();
        if (sslSetting.getCertificateFile() != null) {
            sslContextBuilder.trustManager(new File(sslSetting.getCertificateFile()));
        }
        if (sslSetting.getServerName() != null) {
            serverName = sslSetting.getServerName(); // override
        }
        SslContext sslContext = sslContextBuilder.build();
        SslHandler sslHandler = sslContext.newHandler(ch.alloc(), serverName, config.getPort());
        if (sslSetting.isVerifyHostname()) {
            SSLEngine sslEngine = sslHandler.engine();
            SSLParameters sslParameters = sslEngine.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParameters);
        }
        ch.pipeline().addLast(sslHandler);
    }

    static boolean addWebSocketHandlers(Channel ch, ServerConfig config) throws URISyntaxException {
        if (config.getWs() != null) {
            ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(0xfffff), buildWebSocketHandler(config));
            return true;
        }
        return false;
    }

    private static WebSocketClientProtocolHandler buildWebSocketHandler(ServerConfig config) throws URISyntaxException {
        Optional<WebSocketSetting> ws = Optional.ofNullable(config.getWs());
        String path = ws.map(WebSocketSetting::getPath).orElseThrow(() -> new IllegalArgumentException("required path not present"));
        WebSocketClientProtocolConfig.Builder builder = WebSocketClientProtocolConfig.newBuilder()
            .webSocketUri(new URI("ws", null, config.getHost(), config.getPort(), path, null, null));
        ws.map(WebSocketSetting::getHeader).ifPresent(h -> {
            HttpHeaders headers = new DefaultHttpHeaders();
            for (Map.Entry<String, String> entry : h.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }
            builder.generateOriginHeader(false).customHeaders(headers);
        });
        builder.maxFramePayloadLength(0xfffff);
        return new WebSocketClientProtocolHandler(builder.build());
    }
}
