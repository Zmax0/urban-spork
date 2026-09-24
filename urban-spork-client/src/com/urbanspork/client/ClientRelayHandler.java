package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.ChannelCloseUtil;
import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.dns.IpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientRelayHandler.class);

    record InboundReady(Consumer<Channel> success, Consumer<Channel> failure) {}

    default Consumer<Channel> outboundReady(Channel ignore) {
        return _ -> {};
    }

    default InboundReady inboundReady() {
        return new InboundReady(_ -> {}, _ -> {});
    }

    default void handleFailure(Channel inbound) {
        inboundReady().failure().accept(inbound);
        ChannelCloseUtil.closeOnFlush(inbound);
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
                        ctx.channel().writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }); // L → R
                relayHandler.inboundReady().success().accept(inbound);
                relayHandler.outboundReady(inbound).accept(ctx.channel());
            }
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                logger.error("Connect proxy websocket server {}:{} time out", config.getHost(), config.getPort());
                relayHandler.inboundReady().failure().accept(inbound);
                ChannelCloseUtil.closeOnFlush(inbound);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

    static boolean addWebSocketHandlers(Channel ch, ClientChannelContext context) throws URISyntaxException {
        if (context.config().getWs() != null) {
            ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(0xfffff), buildWebSocketHandler(context));
            return true;
        }
        return false;
    }

    static void addChannelTrafficHandler(Channel outbound, MaybeResolved address, ClientChannelContext context) {
        outbound.pipeline().addLast(new ClientChannelTrafficHandler(address.original().getHostString(), address.original().getPort(), context));
    }

    private static WebSocketClientProtocolHandler buildWebSocketHandler(ClientChannelContext context) throws URISyntaxException {
        ServerConfig config = context.config();
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

    static Promise<String> resolveServerHost(EventLoopGroup group, ClientChannelContext context) {
        ServerConfig config = context.config();
        Promise<String> promise = group.next().newPromise();
        String host = config.getHost();
        DnsSetting dnsSetting = config.getDns();
        if (dnsSetting != null && canResolve(host)) {
            Optional<String> op = dnsSetting.cache().get(host, Instant.now());
            if (op.isPresent()) {
                promise.trySuccess(op.get());
            } else {
                ClientDnsRelayHandler.dohQuery(group, context, dnsSetting.nameServer(), host).addListener((GenericFutureListener<Future<IpResponse>>) f -> {
                    if (f.isSuccess()) {
                        IpResponse resolved = f.get();
                        logger.info("resolved host {} -> {}", host, resolved);
                        dnsSetting.cache().put(host, resolved.ip(), resolved.ttl(), Instant.now());
                        promise.trySuccess(resolved.ip());
                    } else {
                        Throwable t = f.cause();
                        logger.warn("resolve server host {} failed", host, t);
                        promise.trySuccess(host);
                    }
                });
            }
        } else {
            promise.trySuccess(host);
        }
        return promise;
    }

    static boolean canResolve(String host) {
        return !InetAddress.getLoopbackAddress().getHostName().equals(host) && !NetUtil.isValidIpV4Address(host) && !NetUtil.isValidIpV6Address(host);
    }

    default Future<QuicStreamChannel> createQuicStreamChannel(QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext context) {
        return quicChannel.newStreamBootstrap().handler(
            new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ClientRelayHandler.addChannelTrafficHandler(ch, dstAddress, context);
                    addProtocolHandler(ch, dstAddress.address(), context);
                }
            }
        ).create();
    }

    static void addProtocolHandler(Channel outbound, InetSocketAddress address, ClientChannelContext context) {
        outbound.pipeline().addLast(newProtocolHandler(address, context));
    }

    private static ChannelHandler newProtocolHandler(InetSocketAddress address, ClientChannelContext context) {
        ServerConfig config = context.config();
        return switch (config.getProtocol()) {
            case vmess -> new ClientAeadCodec(config.getCipher(), address, config.getPassword());
            case trojan -> new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue());
            default -> new TcpRelayCodec(new Context(), config, address, Mode.Client, ServerUserManager.empty());
        };
    }
}
