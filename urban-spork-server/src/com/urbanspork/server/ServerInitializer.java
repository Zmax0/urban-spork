package com.urbanspork.server;

import com.urbanspork.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.Attributes;
import com.urbanspork.config.ServerConfig;
import com.urbanspork.key.ShadowsocksKey;
import com.urbanspork.protocol.ShadowsocksServerProtocolCodec;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private ServerConfig serverConfig;

    public ServerInitializer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.attr(Attributes.CIPHER).set(serverConfig.getCipher().get());
        ch.attr(Attributes.KEY).set(new ShadowsocksKey(serverConfig.getPassword()));
        ch.pipeline()
            .addLast(new ShadowsocksCipherCodec())
            .addLast(new ShadowsocksServerProtocolCodec())
            .addLast(new ServerProcessor());
    }

}
