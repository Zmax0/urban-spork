package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.impl.ShadowsocksCipherCodecs;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksProtocolDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig serverConfig;

    public ServerInitializer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel c) {
        c.pipeline()
                .addLast(ShadowsocksCipherCodecs.get(serverConfig.getCipher(), serverConfig.getPassword()))
                .addLast(new ShadowsocksProtocolDecoder())
                .addLast(new RemoteConnectionHandler());
    }

}
