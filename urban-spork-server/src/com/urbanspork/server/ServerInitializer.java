package com.urbanspork.server;

import com.urbanspork.common.Attributes;
import com.urbanspork.config.ServerConfig;
import com.urbanspork.key.ShadowsocksKey;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig serverConfig;

    public ServerInitializer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel c) throws Exception {
        c.attr(Attributes.CIPHER).set(serverConfig.getCipher().get());
        c.attr(Attributes.KEY).set(new ShadowsocksKey(serverConfig.getPassword()));
        c.pipeline().addLast(new ServerProtocolHandler());
    }

}
