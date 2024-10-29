package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.server.trojan.ServerHeaderDecoder;
import com.urbanspork.server.vmess.ServerAeadCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.MessageToMessageCodec;
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

    private final ServerInitializationContext context;

    public ServerInitializer(ServerInitializationContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException {
        ServerConfig config = context.config();
        addSslHandler(ch, config);
        addWebSocketHandlers(ch, config);
        switch (config.getProtocol()) {
            case vmess -> ch.pipeline().addLast(new ServerAeadCodec(config), new ExceptionHandler(config), new ServerRelayHandler(config));
            case trojan -> ch.pipeline().addLast(new ServerHeaderDecoder(config), new ExceptionHandler(config));
            default -> ch.pipeline().addLast(new TcpRelayCodec(context.context(), config, Mode.Server, context.userManager()), new ExceptionHandler(config), new ServerRelayHandler(config));
        }
    }

    private static void addSslHandler(Channel c, ServerConfig config) throws SSLException {
        Optional<SslSetting> op = Optional.ofNullable(config.getSsl());
        if (op.isPresent()) {
            SslSetting sslSetting = op.get();
            SslContext sslContext = SslContextBuilder.forServer(new File(sslSetting.getCertificateFile()), new File(sslSetting.getKeyFile()), sslSetting.getKeyPassword()).build();
            String serverName = config.getHost();
            if (sslSetting.getServerName() != null) {
                serverName = sslSetting.getServerName();
            }
            SslHandler sslHandler = sslContext.newHandler(c.alloc(), serverName, config.getPort());
            c.pipeline().addLast(sslHandler);
            return;
        }
        if (Protocol.trojan == config.getProtocol()) {
            throw new IllegalArgumentException("required security setting not present");
        }
    }

    private static void addWebSocketHandlers(Channel channel, ServerConfig config) {
        WebSocketSetting setting = config.getWs();
        if (setting == null) {
            return;
        }
        String path = setting.getPath();
        if (path == null) {
            throw new IllegalArgumentException("required path not present");
        }
        channel.pipeline().addLast(
            new HttpServerCodec(),
            new WebSocketServerProtocolHandler(path, null, true, 0xfffff),
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
