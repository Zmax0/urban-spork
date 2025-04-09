package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.dns.Cache;
import com.urbanspork.common.protocol.dns.DnsRequest;
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
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

public interface ClientTcpRelayHandler extends ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientTcpRelayHandler.class);
    Cache PEER_CACHE = new Cache(256);

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
        String serverHost = ClientRelayHandler.resolveServerHost(inbound.eventLoop().parent(), config);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, config.getPort());
        DnsSetting dnsSetting = config.getDns();
        String dstHost = dst.getHostString();
        if (dnsSetting != null && ClientRelayHandler.canResolve(dstHost)) {
            if (PEER_CACHE.containsKey(dstHost)) {
                ChannelHandler handler = getOutboundChannelHandler(inbound, InetSocketAddress.createUnresolved(PEER_CACHE.get(dstHost), dst.getPort()), config);
                connect(inbound, handler, serverAddress, isReadyOnceConnected);
            } else {
                Promise<String> promise = inbound.eventLoop().parent().next().newPromise();
                promise.addListener((GenericFutureListener<Future<String>>) f1 -> {
                    ChannelHandler handler;
                    if (f1.isSuccess()) {
                        String resolved = f1.get();
                        logger.info("resolved host (on peer side) {} -> {}", dstHost, resolved);
                        PEER_CACHE.put(dstHost, resolved);
                        handler = getOutboundChannelHandler(inbound, InetSocketAddress.createUnresolved(resolved, dst.getPort()), config);
                        connect(inbound, handler, serverAddress, isReadyOnceConnected);
                    } else {
                        logger.info("resolve host {} (on peer side) failed", dstHost, f1.cause());
                        handleFailure(inbound);
                    }
                });
                DnsRequest<FullHttpRequest> dohRequest = Doh.getRequest(dnsSetting.getNameServer(), dstHost, dnsSetting.getSsl());
                newOutboundChannel(inbound, getDohRequestHandler(dohRequest, promise, config), serverAddress).addListener(
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
            ChannelHandler handler = getOutboundChannelHandler(inbound, dst, config);
            connect(inbound, handler, serverAddress, isReadyOnceConnected);
        }
    }

    private void connect(Channel inbound, ChannelHandler handler, InetSocketAddress serverAddress, boolean isReadyOnceConnected) {
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

    private void quic(Channel inbound, InetSocketAddress dstAddress, ServerConfig config) {
        ClientRelayHandler.quicEndpoint(config.getSsl(), inbound.eventLoop()).addListener((ChannelFutureListener) f0 -> {
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel c0 = f0.channel();
            QuicChannel.newBootstrap(c0).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter()).connect().addListener(f1 -> {
                    if (f1.isSuccess()) {
                        QuicChannel quicChannel = (QuicChannel) f1.get();
                        quicChannel.newStreamBootstrap().handler(
                            new ChannelInitializer<>() {
                                @Override
                                protected void initChannel(Channel ch) {
                                    addProtocolHandler(dstAddress, config, ch);
                                }
                            }
                        ).create().addListener(f2 -> {
                            QuicStreamChannel outbound = (QuicStreamChannel) f2.get();
                            outbound.pipeline().addLast(new DefaultChannelInboundHandler(inbound)); // R → L
                            inbound.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                            inboundReady().success().accept(inbound);
                            outboundReady(inbound).accept(outbound);
                        });
                    } else {
                        logger.error("connect relay server {} failed", serverAddress, f1.cause());
                        c0.close();
                        handleFailure(inbound);
                    }
                }
            );
        });
    }

    private ChannelHandler getDohRequestHandler(DnsRequest<FullHttpRequest> request, Promise<String> promise, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, config);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, config)) {
                    outbound.pipeline().addLast(new DohRequestWebSocketCodec(request, promise));
                }
                addProtocolHandler(request.address(), config, outbound);
            }
        };
    }

    private ChannelHandler getOutboundChannelHandler(Channel inbound, InetSocketAddress address, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, config);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, config)) {
                    outbound.pipeline().addLast(new WebSocketCodec(inbound, config, ClientTcpRelayHandler.this));
                    inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
                }
                addProtocolHandler(address, config, outbound);
            }
        };
    }

    class DohRequestWebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final DnsRequest<FullHttpRequest> request;
        private final Promise<String> promise;

        public DohRequestWebSocketCodec(DnsRequest<FullHttpRequest> request, Promise<String> promise) {
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

    private static void addProtocolHandler(InetSocketAddress address, ServerConfig config, Channel outbound) {
        outbound.pipeline().addLast(newProtocolHandler(address, config));
    }

    private static ChannelHandler newProtocolHandler(InetSocketAddress address, ServerConfig config) {
        return switch (config.getProtocol()) {
            case vmess -> new ClientAeadCodec(config.getCipher(), address, config.getPassword());
            case trojan -> new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue());
            default -> new TcpRelayCodec(new Context(), config, address, Mode.Client, ServerUserManager.empty());
        };
    }
}
