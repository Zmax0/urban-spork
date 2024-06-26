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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;

public class ClientSocksConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksConnectHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        Channel inboundChannel = ctx.channel();
        ServerConfig config = inboundChannel.attr(AttributeKeys.SERVER_CONFIG).get();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        new Bootstrap()
            .group(inboundChannel.eventLoop())
            .channel(inboundChannel.getClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(getOutboundChannelHandler(request, config))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(new DefaultChannelInboundHandler(inboundChannel)); // R → L
                    inboundChannel.pipeline().remove(ClientSocksConnectHandler.class);
                    Socks5CommandRequest bndRequest = Socks5.toCommandRequest(Socks5CommandType.CONNECT, (InetSocketAddress) inboundChannel.localAddress());
                    inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, bndRequest.dstAddrType(), bndRequest.dstAddr(), bndRequest.dstPort()))
                        .addListener((ChannelFutureListener) channelFuture -> inboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(outbound))); // L → R
                } else {
                    logger.error("Connect proxy server {} failed", serverAddress);
                    inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    ChannelCloseUtils.closeOnFlush(inboundChannel);
                }
            });
    }

    private static ChannelHandler getOutboundChannelHandler(Socks5CommandRequest request, ServerConfig config) {
        return switch (config.getProtocol()) {
            case vmess -> new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) {
                    InetSocketAddress address = InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort());
                    channel.pipeline().addLast(new ClientAeadCodec(config.getCipher(), address, config.getPassword()));
                }
            };
            case trojan -> new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) throws SSLException {
                    InetSocketAddress address = InetSocketAddress.createUnresolved(request.dstAddr(), request.dstPort());
                    channel.pipeline().addLast(
                        ClientSocksInitializer.buildSslHandler(channel, config),
                        new ClientHeaderEncoder(config.getPassword(), address, SocksCmdType.CONNECT.byteValue())
                    );
                }
            };
            default -> new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addLast(new TcpRelayCodec(new Context(), config, request, Mode.Client));
                }
            };
        };
    }
}