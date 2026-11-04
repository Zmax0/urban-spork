package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.concurrent.TimeUnit;

public record ServerChannelContext(ServerConfig config, Context context, ServerUserManager userManager,
                                   SslContext sslContext, QuicSslContext quicContext) {

    public ServerChannelContext(ServerConfig config, Context context) throws SSLException {
        this(config, context, ServerUserManager.from(config), buildSslContext(config), buildQuicContext(config));
    }

    public void addSslHandler(Channel channel) {
        if (sslContext == null) {
            if (Protocol.trojan == config.getProtocol()) {
                throw new IllegalArgumentException("required security setting not present");
            }
            return;
        }
        SslSetting sslSetting = config.getSsl();
        String serverName = sslSetting.getServerName() == null ? config.getHost() : sslSetting.getServerName();
        SslHandler sslHandler = sslContext.newHandler(channel.alloc(), serverName, config.getPort());
        sslHandler.setCloseNotifyReadTimeout(1, TimeUnit.SECONDS);
        channel.pipeline().addLast(sslHandler);
    }

    private static SslContext buildSslContext(ServerConfig config) throws SSLException {
        SslSetting sslSetting = config.getSsl();
        if (sslSetting == null) {
            return null;
        }
        return SslContextBuilder.forServer(new File(sslSetting.getCertificateFile()), new File(sslSetting.getKeyFile()), sslSetting.getKeyPassword()).build();
    }

    private static QuicSslContext buildQuicContext(ServerConfig config) {
        SslSetting sslSetting = config.getSsl();
        if (sslSetting == null || !config.quicEnabled()) {
            return null; // keep quic native library untouched unless quic is really in use
        }
        return QuicSslContextBuilder.forServer(new File(sslSetting.getKeyFile()), sslSetting.getKeyPassword(), new File(sslSetting.getCertificateFile()))
            .applicationProtocols(ApplicationProtocolNames.HTTP_1_1)
            .build();
    }
}
