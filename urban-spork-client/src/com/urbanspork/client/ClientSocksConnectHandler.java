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
import com.urbanspork.common.protocol.socks.Socks5;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;

public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel inboundChannel = ctx.channel();
        ServerConfig config = inboundChannel.attr(AttributeKeys.SERVER_CONFIG).get();
        boolean autoResponse = !config.wsEnabled();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        new Bootstrap()
            .group(inboundChannel.eventLoop())
            .channel(inboundChannel.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(getOutboundChannelHandler(inboundChannel, request, config))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new DefaultChannelInboundHandler(inboundChannel)); // R → L
                    inboundChannel.pipeline().remove(ClientSocksConnectHandler.class);
                    if (autoResponse) {
                        Socks5CommandRequest bndRequest = Socks5.toCommandRequest(Socks5CommandType.CONNECT, (InetSocketAddress) inboundChannel.localAddress());
                        inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndRequest.dstAddrType(), bndRequest.dstAddr(), bndRequest.dstPort()))
                            .addListener((ChannelFutureListener) channelFuture -> inboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(outbound))); // L → R
                    }
                } else {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    ChannelCloseUtils.closeOnFlush(inboundChannel);
                }
            });
    }

    private static ChannelHandler getOutboundChannelHandler(Channel inbound, Socks5CommandRequest request, ServerConfig config) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel outbound) throws Exception {
                if (config.wsEnabled()) {
                    enableWebSocket(inbound, outbound, config, request);
                }
                switch (config.getProtocol()) {
                    case vmess -> {
                        InetSocketAddress address = InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort());
                        outbound.pipeline().addLast(new ClientAeadCodec(config.getCipher(), address, config.getPassword()));
                    }
                    case trojan -> {
                        InetSocketAddress address = InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort());
                        outbound.pipeline().addLast(
                            ClientSocksInitializer.buildSslHandler(outbound, config),
                            new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue())
                        );
                    }
                    default -> outbound.pipeline().addLast(new TcpRelayCodec(new Context(), config, request, Mode.Client));
                }
            }
        };
    }

    private static void enableWebSocket(Channel inbound, Channel outbound, ServerConfig config, Socks5CommandRequest request) throws URISyntaxException {
        outbound.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(0xffff),
            ClientSocksInitializer.buildWebSocketHandler(config),
            new WebSocketCodec(inbound, config, request)
        );
        inbound.closeFuture().addListener(future -> outbound.writeAndFlush(new CloseWebSocketFrame()));
    }

    static class WebSocketCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {
        private final Channel inbound;
        private final ServerConfig config;
        private final Socks5CommandRequest request;

        public WebSocketCodec(Channel inbound, ServerConfig config, Socks5CommandRequest request) {
            this.inbound = inbound;
            this.config = config;
            this.request = request;
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
                Socks5CommandRequest bndRequest = Socks5.toCommandRequest(Socks5CommandType.CONNECT, (InetSocketAddress) inbound.localAddress());
                inbound.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndRequest.dstAddrType(), bndRequest.dstAddr(), bndRequest.dstPort()));
            }
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                logger.error("Connect proxy websocket server {}:{} time out", config.getHost(), config.getPort());
                inbound.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                ChannelCloseUtils.closeOnFlush(inbound);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}