package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.dns.Cache;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.util.Doh;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ClientTcpRelayHandler extends ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientTcpRelayHandler.class);

    default void connect(Channel inbound, InetSocketAddress dstAddress) {
        ClientChannelContext context = inbound.attr(ClientChannelContext.KEY).get();
        if (context.config().quicEnabled()) {
            quic(inbound, dstAddress, context);
        } else {
            tcp(inbound, dstAddress, context);
        }
    }

    private void tcp(Channel inbound, InetSocketAddress dst, ClientChannelContext context) {
        ServerConfig config = context.config();
        boolean isReadyOnceConnected = config.getWs() == null;
        String serverHost = ClientRelayHandler.tryResolveServerHost(inbound.eventLoop().parent(), config);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, config.getPort());
        DnsSetting dnsSetting = config.getDns();
        String dstHost = dst.getHostString();
        if (dnsSetting != null && ClientRelayHandler.canResolve(dstHost)) {
            Cache cache = dnsSetting.cache();
            Optional<String> dstHostCache = cache.get(dstHost, Instant.now());
            if (dstHostCache.isPresent()) {
                String cached = dstHostCache.get();
                MaybeResolved maybeResolved = new MaybeResolved(dst, InetSocketAddress.createUnresolved(cached, dst.getPort()));
                logger.debug("[tcp] resolve host (in cache) {} -> {}", dstHost, cached);
                connectTcp(inbound, context, serverAddress, maybeResolved, isReadyOnceConnected);
            } else {
                Promise<IpResponse> promise = inbound.eventLoop().parent().next().newPromise();
                promise.addListener((GenericFutureListener<Future<IpResponse>>) f1 -> {
                    if (f1.isSuccess()) {
                        IpResponse resolved = f1.get();
                        logger.info("[tcp] resolve host (on peer) {} -> {}", dstHost, resolved);
                        cache.put(dstHost, resolved.ip(), resolved.ttl(), Instant.now());
                        MaybeResolved maybeResolved = new MaybeResolved(dst, InetSocketAddress.createUnresolved(resolved.ip(), dst.getPort()));
                        connectTcp(inbound, context, serverAddress, maybeResolved, isReadyOnceConnected);
                    } else {
                        logger.info("[tcp] resolve host {} (on peer) failed", dstHost, f1.cause());
                        handleFailure(inbound);
                    }
                });
                DnsRequest<FullHttpRequest> dohRequest = Doh.getRequest(dnsSetting.nameServer(), dstHost, dnsSetting.ssl());
                newOutboundChannel(inbound, getDohRequestHandler(dohRequest, promise, context), serverAddress).addListener(
                    (ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            if (isReadyOnceConnected) {
                                Doh.query(future.channel(), dohRequest, promise);
                            }
                        } else {
                            logger.error("connect relay server {} failed, dns request", serverAddress, future.cause());
                            handleFailure(inbound);
                        }
                    });
            }
        } else {
            connectTcp(inbound, context, serverAddress, new MaybeResolved(dst), isReadyOnceConnected);
        }
    }

    private void connectTcp(Channel inbound, ClientChannelContext context, InetSocketAddress serverAddress, MaybeResolved dstAddress, boolean isReadyOnceConnected) {
        ChannelHandler handler = getOutboundChannelHandler(inbound, dstAddress, context);
        newOutboundChannel(inbound, handler, serverAddress).addListener(
            (ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    tcpOutboundReady(inbound, future.channel(), isReadyOnceConnected);
                } else {
                    logger.error("connect relay server {} failed", serverAddress, future.cause());
                    handleFailure(inbound);
                }
            }
        );
    }

    private ChannelFuture newOutboundChannel(Channel inbound, ChannelHandler handler, InetSocketAddress server) {
        return new Bootstrap()
            .group(inbound.eventLoop())
            .channel(inbound.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(handler)
            .connect(server);
    }

    private void tcpOutboundReady(Channel inbound, Channel outbound, boolean isReadyOnceConnected) {
        outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
        if (isReadyOnceConnected) {
            inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
            inboundReady().success().accept(inbound);
            outboundReady(inbound).accept(outbound);
        }
    }

    private void quic(Channel inbound, InetSocketAddress dstAddress, ClientChannelContext context) {
        ServerConfig config = context.config();
        ClientRelayHandler.quicEndpoint(config.getSsl(), inbound.eventLoop()).addListener((ChannelFutureListener) f0 -> {
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel quicEndpoint = f0.channel();
            QuicChannel.newBootstrap(quicEndpoint).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter()).connect().addListener(f1 -> {
                    if (f1.isSuccess()) {
                        QuicChannel quicChannel = (QuicChannel) f1.get();
                        inbound.closeFuture().addListener(f -> quicChannel.close());
                        connectQuic(inbound, dstAddress, context, quicChannel, quicEndpoint);
                    } else {
                        logger.error("connect relay server {} failed", serverAddress, f1.cause());
                        quicEndpoint.close();
                        handleFailure(inbound);
                    }
                }
            );
        });
    }

    private void connectQuic(Channel inbound, InetSocketAddress dstAddress, ClientChannelContext context, QuicChannel quicChannel, Channel quicEndpoint) {
        DnsSetting dnsSetting = context.config().getDns();
        String dstHost = dstAddress.getHostString();
        if (dnsSetting != null && ClientRelayHandler.canResolve(dstHost)) {
            Cache cache = dnsSetting.cache();
            Optional<String> cacheDstHost = cache.get(dstHost, Instant.now());
            if (cacheDstHost.isPresent()) {
                String cached = cacheDstHost.get();
                logger.debug("[quic] resolve host (in cache) {} -> {}", dstHost, cached);
                MaybeResolved maybeResolved = new MaybeResolved(dstAddress, InetSocketAddress.createUnresolved(cached, dstAddress.getPort()));
                connectQuic0(inbound, quicChannel, maybeResolved, context);
            } else {
                DnsRequest<FullHttpRequest> dohRequest = Doh.getRequest(dnsSetting.nameServer(), dstHost, dnsSetting.ssl());
                createQuicStreamChannel(quicChannel, new MaybeResolved(dohRequest.address()), context).addListener(f2 -> {
                    QuicStreamChannel outbound = (QuicStreamChannel) f2.get();
                    Promise<IpResponse> promise = inbound.eventLoop().parent().next().newPromise();
                    promise.addListener((GenericFutureListener<Future<IpResponse>>) f3 -> {
                        if (f3.isSuccess()) {
                            IpResponse resolved = f3.get();
                            logger.info("[quic] resolve host (on peer) {} -> {}", dstHost, resolved);
                            cache.put(dstHost, resolved.ip(), resolved.ttl(), Instant.now());
                            MaybeResolved maybeResolved = new MaybeResolved(dstAddress, InetSocketAddress.createUnresolved(resolved.ip(), dstAddress.getPort()));
                            connectQuic0(inbound, quicChannel, maybeResolved, context);
                        } else {
                            logger.info("[quic] resolve host {} (on peer) failed", dstHost, f3.cause());
                            outbound.close();
                            quicChannel.close();
                            quicEndpoint.close();
                            handleFailure(inbound);
                        }
                    });
                    Doh.query(outbound, dohRequest, promise);
                });
            }
        } else {
            connectQuic0(inbound, quicChannel, new MaybeResolved(dstAddress), context);
        }
    }

    private void connectQuic0(Channel inbound, QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext context) {
        createQuicStreamChannel(quicChannel, dstAddress, context).addListener(f2 -> quicOutboundReady(inbound, (QuicStreamChannel) f2.get()));
    }

    private static Future<QuicStreamChannel> createQuicStreamChannel(QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext context) {
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

    private void quicOutboundReady(Channel inbound, QuicStreamChannel outbound) {
        outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
        inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
        inboundReady().success().accept(inbound);
        outboundReady(inbound).accept(outbound);
    }

    private ChannelInitializer<Channel> getDohRequestHandler(DnsRequest<FullHttpRequest> request, Promise<IpResponse> promise, ClientChannelContext context) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, context);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, context)) {
                    outbound.pipeline().addLast(new DohRequestWebSocketCodec(request, promise));
                }
                addProtocolHandler(outbound, request.address(), context);
            }
        };
    }

    private ChannelInitializer<Channel> getOutboundChannelHandler(Channel inbound, MaybeResolved address, ClientChannelContext context) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, context);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, context)) {
                    outbound.pipeline().addLast(new WebSocketCodec(inbound, context.config(), ClientTcpRelayHandler.this));
                    inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
                }
                ClientRelayHandler.addChannelTrafficHandler(outbound, address, context);
                addProtocolHandler(outbound, address.address(), context);
            }
        };
    }

    class DohRequestWebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final DnsRequest<FullHttpRequest> request;
        private final Promise<IpResponse> promise;

        public DohRequestWebSocketCodec(DnsRequest<FullHttpRequest> request, Promise<IpResponse> promise) {
            this.request = request;
            this.promise = promise;
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
                Doh.query(ctx.channel(), request, promise);
            }
        }
    }

    private static void addProtocolHandler(Channel outbound, InetSocketAddress address, ClientChannelContext context) {
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
