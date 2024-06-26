package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.io.File;

public class ClientSocksInitializer extends ChannelInitializer<NioSocketChannel> {

    private final ServerConfig config;

    public ClientSocksInitializer(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        channel.attr(AttributeKeys.SERVER_CONFIG).set(config);
        channel.pipeline()
            .addLast(config.getTrafficShapingHandler())
            .addLast(new SocksPortUnificationServerHandler())
            .addLast(ClientSocksMessageHandler.INSTANCE);
    }

    public static SslHandler buildSslHandler(Channel ch, ServerConfig config) throws SSLException {
        String serverName = config.getHost();
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        boolean verifyHostname = true;
        if (config.getSsl() != null) {
            SslSetting ssl = config.getSsl();
            if (ssl.getCertificateFile() != null) {
                sslContextBuilder.trustManager(new File(ssl.getCertificateFile()));
            }
            if (ssl.getServerName() != null) {
                serverName = ssl.getServerName(); // override
            }
            verifyHostname = ssl.isVerifyHostname();
        }
        SslContext sslContext = sslContextBuilder.build();
        SslHandler sslHandler = sslContext.newHandler(ch.alloc(), serverName, config.getPort());
        if (verifyHostname) {
            SSLEngine sslEngine = sslHandler.engine();
            SSLParameters sslParameters = sslEngine.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParameters);
        }
        return sslHandler;
    }
}
