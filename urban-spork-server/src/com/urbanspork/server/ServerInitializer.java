package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.server.vmess.ServerAeadCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig config;
    private final Context context;

    public ServerInitializer(ServerConfig config, Context context) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected void initChannel(Channel c) {
        ChannelPipeline pipeline = c.pipeline();
        if (Protocol.vmess == config.getProtocol()) {
            pipeline.addLast(new ServerAeadCodec(config));
        } else {
            pipeline.addLast(new TcpRelayCodec(context, config, Mode.Server));
        }
        pipeline.addLast(new ServerRelayHandler(config), new ExceptionHandler(config));
    }
}
