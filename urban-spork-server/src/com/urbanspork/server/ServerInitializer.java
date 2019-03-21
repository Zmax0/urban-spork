package com.urbanspork.server;

import com.urbanspork.cipher.AES_256_CBA;
import com.urbanspork.cipher.ShadowsocksCipherCodec;
import com.urbanspork.common.Attributes;
import com.urbanspork.key.ShadowsocksKey;
import com.urbanspork.protocol.ShadowsocksServerProtocolCodec;
import com.urbanspork.utils.ConfigUtils;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        String password = ConfigUtils.get("socks5.server.password", String.class);
        ch.attr(Attributes.CIPHER).set(new AES_256_CBA());
        ch.attr(Attributes.KEY).set(new ShadowsocksKey(password));
        ch.pipeline()
            .addLast(new ShadowsocksCipherCodec())
            .addLast(new ShadowsocksServerProtocolCodec())
            .addLast(new ServerProcessor());
    }

}
