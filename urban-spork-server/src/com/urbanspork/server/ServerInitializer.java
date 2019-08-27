package com.urbanspork.server;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksCipherCodec;
import com.urbanspork.cipher.ShadowsocksKey;
import com.urbanspork.common.Attributes;
import com.urbanspork.config.ServerConfig;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig serverConfig;

    public ServerInitializer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel c) throws Exception {
        ShadowsocksCipher cipher = serverConfig.getCipher().newShadowsocksCipher();
        c.attr(Attributes.CIPHER).set(cipher);
        c.attr(Attributes.KEY).set(new ShadowsocksKey(serverConfig.getPassword(), cipher.getKeyLength()));
        c.pipeline()
            .addLast(new ShadowsocksCipherCodec())
            .addLast(new ServerProtocolHandler());
    }

}
