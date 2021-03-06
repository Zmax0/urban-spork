package com.urbanspork.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksKey;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

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
            logger.error("Proxy server configuration is unreachale");
            channel.disconnect();
        } else {
            channel.attr(AttributeKeys.SERVER_ADDRESS).set(new InetSocketAddress(config.getHost(), Integer.parseInt(config.getPort())));
            ShadowsocksCipher cipher = config.getCipher().newShadowsocksCipher();
            channel.attr(AttributeKeys.CIPHER).set(cipher);
            channel.attr(AttributeKeys.KEY).set(new ShadowsocksKey(config.getPassword(), cipher.getKeySize()));
            channel.pipeline()
                .addLast(new SocksPortUnificationServerHandler())
                .addLast(new ClientSocksMessageHandler());
        }
    }

}
