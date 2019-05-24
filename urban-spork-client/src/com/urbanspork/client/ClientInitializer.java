package com.urbanspork.client;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksKey;
import com.urbanspork.common.Attributes;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ServerConfig;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

public class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(ClientInitializer.class);

    private ClientConfig clientConfig;

    public ClientInitializer(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        ServerConfig config = clientConfig.getCurrent();
        if (config == null) {
            logger.error("Proxy server configuration is unreachale");
            channel.disconnect();
        } else {
            channel.attr(Attributes.SERVER_ADDRESS).set(new InetSocketAddress(config.getHost(), Integer.valueOf(config.getPort())));
            ShadowsocksCipher cipher = config.getCipher().get();
            channel.attr(Attributes.CIPHER).set(cipher);
            channel.attr(Attributes.KEY).set(new ShadowsocksKey(config.getPassword(), cipher.getKeyLength()));
            channel.pipeline()
                .addLast(new SocksPortUnificationServerHandler())
                .addLast(new ClientShakeHandsHandler());
        }
    }

}
