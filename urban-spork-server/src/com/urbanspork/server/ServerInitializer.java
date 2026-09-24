package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.WebSocketSetting;
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

import java.util.List;
import java.util.Objects;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerChannelContext context;

    public ServerInitializer(ServerChannelContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(Channel ch) {
        ServerConfig config = context.config();
        context.addSslHandler(ch);
        addWebSocketHandlers(ch, config);
        addProtocolHandlers(ch, context);
    }

    static void addProtocolHandlers(Channel ch, ServerChannelContext context) {
        ServerConfig config = context.config();
        switch (config.getProtocol()) {
            case vmess -> ch.pipeline().addLast(new ServerAeadCodec(config), new ServerRelayHandler(config), new ExceptionHandler(config));
            case trojan -> ch.pipeline().addLast(new ServerHeaderDecoder(config), new ExceptionHandler(config));
            default -> ch.pipeline().addLast(new TcpRelayCodec(context.context(), config, Mode.Server, context.userManager()), new ExceptionHandler(config), new ServerRelayHandler(config));
        }
    }

    private static void addWebSocketHandlers(Channel channel, ServerConfig config) {
        WebSocketSetting setting = config.getWs();
        if (setting == null) {
            return;
        }
        String path = Objects.requireNonNull(setting.getPath(), "required path not present");
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
