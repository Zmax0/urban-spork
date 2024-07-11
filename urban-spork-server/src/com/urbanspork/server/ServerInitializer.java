package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.server.trojan.ServerHeaderDecoder;
import com.urbanspork.server.vmess.ServerAeadCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig config;
    private final Context context;

    public ServerInitializer(ServerConfig config, Context context) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected void initChannel(Channel c) throws SSLException {
        if (config.wsEnabled()) {
            enableWebSocket(c);
        }
        switch (config.getProtocol()) {
            case vmess -> c.pipeline().addLast(new ServerAeadCodec(config), new ExceptionHandler(config), new ServerRelayHandler(config));
            case trojan -> {
                String serverName = config.getHost();
                SslSetting sslSetting = Optional.ofNullable(config.getSsl()).orElseThrow(() -> new IllegalArgumentException("required security setting not present"));
                SslContext sslContext = SslContextBuilder.forServer(new File(sslSetting.getCertificateFile()), new File(sslSetting.getKeyFile()), sslSetting.getKeyPassword()).build();
                if (sslSetting.getServerName() != null) {
                    serverName = sslSetting.getServerName();
                }
                SslHandler sslHandler = sslContext.newHandler(c.alloc(), serverName, config.getPort());
                c.pipeline().addLast(sslHandler, new ServerHeaderDecoder(config), new ExceptionHandler(config));
            }
            default -> c.pipeline().addLast(new TcpRelayCodec(context, config, Mode.Server), new ExceptionHandler(config), new ServerRelayHandler(config));
        }
    }

    private void enableWebSocket(Channel channel) {
        String path = Optional.ofNullable(config.getWs()).map(WebSocketSetting::getPath).orElseThrow(() -> new IllegalArgumentException("required path not present"));
        channel.pipeline().addLast(
            new HttpServerCodec(),
            new HttpObjectAggregator(0xffff),
            new WebSocketServerProtocolHandler(path),
            new MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf>() {
                @Override
                protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
                    out.add(new BinaryWebSocketFrame(msg.retain()));
                }

                @Override
                protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {
                    out.add(msg.retain().content());
                }
            }
        );
    }
}
