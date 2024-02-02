package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.TCPRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.server.vmess.ServerAEADCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig config;

    public ServerInitializer(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(Channel c) {
        ChannelPipeline pipeline = c.pipeline();
        if (Protocols.vmess == config.getProtocol()) {
            pipeline.addLast(new ServerAEADCodec(config));
        } else {
            pipeline.addLast(new TCPRelayCodec(Mode.Server, config));
        }
        pipeline.addLast(new RemoteConnectHandler(config), new ExceptionHandler(config, Mode.Server));
    }
}
