package com.urbanspork.client;

import com.urbanspork.client.trojan.ClientHeaderEncoder;
import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public interface ClientTcpRelayHandler extends ClientRelayHandler {
    Logger logger = LoggerFactory.getLogger(ClientTcpRelayHandler.class);

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
                    logger.error("connect relay server {} failed", serverAddress, future.cause());
                    handleFailure(inbound);
                }
            });
    }

    private void quic(Channel inbound, InetSocketAddress dstAddress, ServerConfig config) {
        ClientRelayHandler.quicEndpoint(config.getSsl(), inbound.eventLoop()).addListener((ChannelFutureListener) f0 -> {
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel c0 = f0.channel();
            QuicChannel.newBootstrap(c0).remoteAddress(serverAddress).streamHandler(new ChannelInboundHandlerAdapter() {}).connect().addListener(f1 -> {
                    if (f1.isSuccess()) {
                        QuicChannel quicChannel = (QuicChannel) f1.get();
                        quicChannel.newStreamBootstrap().handler(
                            new ChannelInitializer<>() {
                                @Override
                                protected void initChannel(Channel ch) {
                                    addOutboundProtocolHandler(dstAddress, config, ch);
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

    private ChannelHandler getOutboundChannelHandler(Channel inbound, InetSocketAddress address, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                ClientRelayHandler.addSslHandler(outbound, config);
                if (ClientRelayHandler.addWebSocketHandlers(outbound, config)) {
                    outbound.pipeline().addLast(new WebSocketCodec(inbound, config, ClientTcpRelayHandler.this));
                    inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
                }
                addOutboundProtocolHandler(address, config, outbound);
            }
        };
    }

    private static void addOutboundProtocolHandler(InetSocketAddress address, ServerConfig config, Channel outbound) {
        switch (config.getProtocol()) {
            case vmess -> outbound.pipeline().addLast(new ClientAeadCodec(config.getCipher(), address, config.getPassword()));
            case trojan -> outbound.pipeline().addLast(new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue()));
            default -> outbound.pipeline().addLast(new TcpRelayCodec(new Context(), config, address, Mode.Client, ServerUserManager.empty()));
        }
    }
}
