package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ClientChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(ClientChannelInitializer.class);

    private final ClientConfig clientConfig;

    public ClientChannelInitializer(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        ServerConfig config = clientConfig.getCurrent();
        if (config == null) {
            logger.error("Proxy server configuration is unreachable");
            channel.disconnect();
        } else {
            Protocols protocol = config.getProtocol();
            channel.attr(AttributeKeys.SERVER_ADDRESS).set(new InetSocketAddress(config.getHost(), Integer.parseInt(config.getPort())));
            channel.attr(AttributeKeys.CIPHER).set(config.getCipher());
            channel.attr(AttributeKeys.PROTOCOL).set(protocol);
            channel.attr(AttributeKeys.PASSWORD).set(config.getPassword());
            channel.pipeline()
                    .addLast(new SocksPortUnificationServerHandler())
                    .addLast(ClientSocksMessageHandler.INSTANCE)
            ;
        }
    }

}
