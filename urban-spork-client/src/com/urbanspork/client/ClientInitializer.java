package com.urbanspork.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.client.config.ClientConfig;
import com.urbanspork.client.config.ServerConfig;
import com.urbanspork.common.Attributes;
import com.urbanspork.key.ShadowsocksKey;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

public class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final Logger logger = LoggerFactory.getLogger(ClientInitializer.class);

    private ClientConfig clientConfig;

    public ClientInitializer(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        ServerConfig config = clientConfig.getCurrent();
        if (config == null) {
            logger.warn("Proxy server configuration is unreachale");
            channel.disconnect();
        } else {
            channel.attr(Attributes.SERVER_ADDRESS).set(new InetSocketAddress(config.getHost(), Integer.valueOf(config.getPort())));
            channel.attr(Attributes.KEY).set(new ShadowsocksKey(config.getPassword()));
            channel.attr(Attributes.CIPHER).set(config.getCipher().get());
            channel.pipeline()
                .addLast(new SocksPortUnificationServerHandler())
                .addLast(new ClientShakeHandsHandler());
        }
    }

}
