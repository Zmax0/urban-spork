package com.urbanspork.client;

import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.dns.Cache;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.util.Doh;
import com.urbanspork.common.util.FutureListeners;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ClientDnsRelayHandler extends ClientRelayHandler {

    String label();

    Future<? extends Channel> newChannel(DnsRequest dohRequest, Promise<IpResponse> ipPromise);

    boolean isReadyOnceConnected();

    ///
    /// callback on success
    ///
    /// @param maybeResolved ip socket address that *may be* resolved
    ///
    void callback1(MaybeResolved maybeResolved);

    ///
    /// callback on failure
    ///
    /// @param t one cause failure
    ///
    void callback0(Throwable t);

    static Promise<IpResponse> dohQuery(EventLoopGroup group, ClientChannelContext context, String nameServer, String domain) {
        DnsRequest request = new DnsRequest(nameServer, domain);
        return Doh.query(group, request, channel -> context.newDohSslHandler(channel, request.address()));
    }

    default void resolve(EventLoop eventLoop, ClientChannelContext context, InetSocketAddress target) {
        if (!target.isUnresolved()) {
            callback1(new MaybeResolved(target, target));
            return;
        }
        DnsSetting dnsSetting = context.config().getDns();
        String host = target.getHostString();
        if (dnsSetting != null && ClientRelayHandler.canResolve(host)) {
            Cache cache = dnsSetting.cache();
            Optional<String> dstHostCache = cache.get(host, Instant.now());
            if (dstHostCache.isPresent()) {
                String cached = dstHostCache.get();
                MaybeResolved maybeResolved = new MaybeResolved(target, InetSocketAddress.createUnresolved(cached, target.getPort()));
                logger.debug("[dns][{}]resolve host (in cache) {} -> {}", label(), host, cached);
                callback1(maybeResolved);
            } else {
                Promise<IpResponse> inner = eventLoop.newPromise();
                inner.addListener((GenericFutureListener<Future<IpResponse>>) f1 -> {
                    if (f1.isSuccess()) {
                        IpResponse resolved = f1.get();
                        logger.info("[dns][{}]resolve host (on peer) {} -> {}", label(), host, resolved);
                        cache.put(host, resolved.ip(), resolved.ttl(), Instant.now());
                        MaybeResolved maybeResolved = new MaybeResolved(target, InetSocketAddress.createUnresolved(resolved.ip(), target.getPort()));
                        callback1(maybeResolved);
                    } else {
                        logger.info("[dns][{}]resolve host {} (on peer) failed", label(), host, f1.cause());
                        callback0(f1.cause());
                    }
                });
                DnsRequest dohRequest = new DnsRequest(dnsSetting.nameServer(), host);
                Future<? extends Channel> outboundChannel = newChannel(dohRequest, inner);
                if (isReadyOnceConnected()) {
                    outboundChannel.addListener(FutureListeners.success(ch ->
                        Doh.query(ch, dohRequest, inner, context.newDohSslHandler(ch, dohRequest.address()))));
                }
            }
        } else {
            callback1(new MaybeResolved(target, null));
        }
    }

    default void newTcpChannel(EventLoopGroup group, InetSocketAddress server, DnsRequest dnsRequest, ClientChannelContext context, Promise<Channel> channelPromise, Promise<IpResponse> ipPromise) {
        ClientTcpRelayHandler.createTcpOutboundChannel(group, getChannelInitializer(dnsRequest, ipPromise, context), server).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channelPromise.setSuccess(f.channel());
            } else {
                Throwable cause = f.cause();
                logger.error("[dns][{}]connect relay server {} failed", label(), server, cause);
                channelPromise.setFailure(cause);
            }
        });
    }

    default void newQuicChannel(EventLoopGroup group, InetSocketAddress server, DnsRequest dohRequest, ClientChannelContext context, Promise<Channel> channelPromise) {
        context.quicEndpoint(group).addListener((ChannelFutureListener) f0 -> {
            Channel quicEndpoint = f0.channel();
            QuicChannel.newBootstrap(quicEndpoint).remoteAddress(server).streamHandler(new ChannelInboundHandlerAdapter()).connect().addListener((GenericFutureListener<Future<QuicChannel>>) f1 -> {
                if (f1.isSuccess()) {
                    createQuicStreamChannel(f1.get(), new MaybeResolved(dohRequest.address()), context).addListener((GenericFutureListener<Future<QuicStreamChannel>>) f2 -> {
                        if (f2.isSuccess()) {
                            channelPromise.setSuccess(f2.get());
                        } else {
                            Throwable cause = f2.cause();
                            logger.error("[dns][{}]create stream channel server failed, relay server {}", label(), server, cause);
                            channelPromise.setFailure(cause);
                        }
                    });
                } else {
                    Throwable cause = f1.cause();
                    logger.error("[dns][{}]create endpoint channel failed, relay server {}", label(), server, cause);
                    quicEndpoint.close();
                    channelPromise.setFailure(cause);
                }
            });
        });
    }

    private static ChannelInitializer<Channel> getChannelInitializer(DnsRequest request, Promise<IpResponse> promise, ClientChannelContext context) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                context.addSslHandler(outbound);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, context)) {
                    Optional.of(context).map(ClientChannelContext::config).map(ServerConfig::getDns).ifPresent(
                        _ -> outbound.pipeline().addLast(new DohRequestWebSocketCodec(request, context, promise))
                    );
                }
                ClientRelayHandler.addProtocolHandler(outbound, request.address(), context);
            }
        };
    }

    class DohRequestWebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final DnsRequest request;
        private final ClientChannelContext context;
        private final Promise<IpResponse> promise;

        public DohRequestWebSocketCodec(DnsRequest request, ClientChannelContext context, Promise<IpResponse> promise) {
            this.request = request;
            this.context = context;
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
                Channel channel = ctx.channel();
                Doh.query(channel, request, promise, context.newDohSslHandler(channel, request.address()));
            }
        }
    }
}
