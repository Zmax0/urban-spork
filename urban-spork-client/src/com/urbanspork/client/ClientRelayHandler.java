package com.urbanspork.client;

import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.dns.Cache;
import com.urbanspork.common.util.Doh;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientRelayHandler.class);
    Cache SERVER_CACHE = new Cache(10);

    record InboundReady(Consumer<Channel> success, Consumer<Channel> failure) {}

    default Consumer<Channel> outboundReady(Channel ignore) {
        return channel -> {};
    }

    default InboundReady inboundReady() {
        return new InboundReady(channel -> {}, channel -> {});
    }

    default void handleFailure(Channel inbound) {
        inboundReady().failure().accept(inbound);
        ChannelCloseUtils.closeOnFlush(inbound);
    }

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

    static ChannelFuture quicEndpoint(SslSetting sslSetting, EventLoopGroup group) {
        QuicSslContext context = QuicSslContextBuilder.forClient().trustManager(new File(sslSetting.getCertificateFile())).applicationProtocols().applicationProtocols("http/1.1").build();
        ChannelHandler codec = new QuicClientCodecBuilder()
            .sslContext(context)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(0xfffff)
            .initialMaxStreamDataBidirectionalLocal(0xfffff)
            .build();
        return new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(codec).bind(0);
    }

    static String resolveServerHost(EventLoopGroup group, ServerConfig config) {
        String host = config.getHost();
        DnsSetting dns = config.getDns();
        if (dns != null && canResolve(host)) {
            String cached = SERVER_CACHE.get(host);
            if (cached != null) {
                return cached;
            }
            try {
                String resolved = Doh.query(group, dns.getNameServer(), host).get(10, TimeUnit.SECONDS);
                logger.info("resolved host {} -> {}", host, resolved);
                SERVER_CACHE.put(host, resolved);
                return resolved;
            } catch (Exception e) {
                logger.error("resolve server host {} failed", host, e);
            }
        }
        return host;
    }

    static boolean canResolve(String host) {
        return !InetAddress.getLoopbackAddress().getHostName().equals(host) && !NetUtil.isValidIpV4Address(host) && !NetUtil.isValidIpV6Address(host);
    }
}
