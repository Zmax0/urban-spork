package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSocksInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocksInitializer.class);

    private final ServerConfig config;
    private final Integer socksPort;

    public ClientSocksInitializer(ServerConfig config, Integer socksPort) {
        this.config = config;
        this.socksPort = socksPort;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        if (config == null) {
            logger.error("Proxy server configuration is unreachable");
            channel.close();
        } else {
            channel.attr(AttributeKeys.SERVER_CONFIG).set(config);
            channel.attr(AttributeKeys.SOCKS_PORT).set(socksPort);
            channel.pipeline()
                    .addLast(new SocksPortUnificationServerHandler())
                    .addLast(ClientSocksMessageHandler.INSTANCE);
        }
    }

}
