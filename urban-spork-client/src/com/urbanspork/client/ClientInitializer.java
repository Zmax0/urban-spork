package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final ServerConfig config;

    ClientInitializer(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        channel.attr(AttributeKeys.SERVER_CONFIG).set(config);
        channel.pipeline().addLast(config.getTrafficShapingHandler(), new ClientProxyUnificationHandler());
    }
}
