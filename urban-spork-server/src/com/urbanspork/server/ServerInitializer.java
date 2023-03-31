package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressDecoder;
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
                .addLast(ShadowsocksAEADCipherCodecs.get(serverConfig.getCipher(), serverConfig.getPassword()))
                .addLast(new ShadowsocksAddressDecoder())
                .addLast(new RemoteConnectionHandler());
    }

}
