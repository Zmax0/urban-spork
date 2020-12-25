package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.cipher.ShadowsocksKey;
import com.urbanspork.common.config.ServerConfig;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig serverConfig;

    public ServerInitializer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel c) {
        ShadowsocksCipher cipher = serverConfig.getCipher().newShadowsocksCipher();
        c.attr(AttributeKeys.CIPHER).set(cipher);
        c.attr(AttributeKeys.KEY).set(new ShadowsocksKey(serverConfig.getPassword(), cipher.getKeySize()));
        c.pipeline()
            .addLast(new ShadowsocksCipherCodec())
            .addLast(new ServerProtocolHandler());
    }

}
