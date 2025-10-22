package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.util.FutureListeners;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;

public interface ClientTcpRelayHandler extends ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientTcpRelayHandler.class);

    default void connect(Channel inbound, InetSocketAddress dstAddress, ClientChannelContext context) {
        if (context.config().quicEnabled()) {
            quic(inbound, dstAddress, context);
        } else {
            tcp(inbound, dstAddress, context);
        }
    }

    private void tcp(Channel inbound, InetSocketAddress dst, ClientChannelContext context) {
        ServerConfig config = context.config();
        boolean isReadyOnceConnected = config.getWs() == null;
        EventLoop eventLoop = inbound.eventLoop();
        String serverHost = ClientRelayHandler.tryResolveServerHost(eventLoop, config);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, config.getPort());
        new ClientDnsRelayHandler() {
            @Override
            public String label() {
                return config.networkText();
            }

            @Override
            public Future<? extends Channel> newChannel(DnsRequest dohRequest, Promise<IpResponse> ipPromise) {
                Promise<Channel> channelPromise = eventLoop.newPromise();
                newTcpChannel(eventLoop, serverAddress, dohRequest, context, channelPromise, ipPromise);
                channelPromise.addListener(FutureListeners.failure(_ -> ClientTcpRelayHandler.this.handleFailure(inbound)));
                return channelPromise;
            }

            @Override
            public boolean isReadyOnceConnected() {
                return isReadyOnceConnected;
            }

            @Override
            public void callback1(MaybeResolved maybeResolved) {
                connectTcp(inbound, context, serverAddress, maybeResolved, isReadyOnceConnected);
            }

            @Override
            public void callback0(Throwable t) {
                ClientTcpRelayHandler.this.handleFailure(inbound);
            }
        }.resolve(eventLoop, config.getDns(), dst);
    }

    private void connectTcp(Channel inbound, ClientChannelContext context, InetSocketAddress serverAddress, MaybeResolved dstAddress, boolean isReadyOnceConnected) {
        ChannelHandler handler = getOutboundChannelHandler(inbound, dstAddress, context);
        createTcpOutboundChannel(inbound.eventLoop(), handler, serverAddress).addListener(
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

    static ChannelFuture createTcpOutboundChannel(EventLoopGroup group, ChannelHandler handler, InetSocketAddress server) {
        return new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
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
                        inbound.closeFuture().addListener(_ -> quicChannel.close());
                        connectQuic(inbound, dstAddress, context, quicChannel, quicEndpoint);
                    } else {
                        logger.error("[quic] create endpoint channel failed, relay server {}", serverAddress, f1.cause());
                        quicEndpoint.close();
                        handleFailure(inbound);
                    }
                }
            );
        });
    }

    private void connectQuic(Channel inbound, InetSocketAddress dstAddress, ClientChannelContext context, QuicChannel quicChannel, Channel quicEndpoint) {
        new ClientDnsRelayHandler() {
            private QuicStreamChannel outbound;

            @Override
            public String label() {
                return "quic";
            }

            @Override
            public Future<QuicStreamChannel> newChannel(DnsRequest dohRequest, Promise<IpResponse> ipPromise) {
                Future<QuicStreamChannel> future = createQuicStreamChannel(quicChannel, new MaybeResolved(dohRequest.address()), context);
                future.addListener(FutureListeners.success(c -> outbound = c));
                return future;
            }

            @Override
            public boolean isReadyOnceConnected() {
                return true;
            }

            @Override
            public void callback1(MaybeResolved maybeResolved) {
                connectQuic0(inbound, quicChannel, maybeResolved, context);
            }

            @Override
            public void callback0(Throwable t) {
                Optional.ofNullable(outbound).ifPresent(QuicStreamChannel::close);
                quicChannel.close();
                quicEndpoint.close();
                ClientTcpRelayHandler.this.handleFailure(inbound);
            }
        }.resolve(inbound.eventLoop(), context.config().getDns(), dstAddress);
    }

    private void connectQuic0(Channel inbound, QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext context) {
        createQuicStreamChannel(quicChannel, dstAddress, context).addListener(f2 -> {
            if (f2.isSuccess()) {
                quicOutboundReady(inbound, (QuicStreamChannel) f2.get());
            } else {
                logger.error("[quic][{}] create stream channel failed", quicChannel);
            }
        });
    }

    static Future<QuicStreamChannel> createQuicStreamChannel(QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext context) {
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

    private ChannelInitializer<Channel> getOutboundChannelHandler(Channel inbound, MaybeResolved address, ClientChannelContext context) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, context);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, context)) {
                    outbound.pipeline().addLast(new WebSocketCodec(inbound, context.config(), ClientTcpRelayHandler.this));
                    inbound.closeFuture().addListener(_ -> outbound.writeAndFlush(new CloseWebSocketFrame()));
                }
                ClientRelayHandler.addChannelTrafficHandler(outbound, address, context);
                addProtocolHandler(outbound, address.address(), context);
            }
        };
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
