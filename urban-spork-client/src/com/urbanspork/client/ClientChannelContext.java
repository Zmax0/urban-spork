package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.util.Doh;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public record ClientChannelContext(ServerConfig config,
                                   GlobalChannelTrafficShapingHandler traffic,
                                   Map<String, ClientChannelTrafficHandler> channelTraffic,
                                   SslContext sslContext,
                                   QuicSslContext quicContext,
                                   SslContext dohSslContext) {

    public ClientChannelContext(
        ServerConfig config, GlobalChannelTrafficShapingHandler traffic,
        Map<String, ClientChannelTrafficHandler> channelTraffic
    ) throws SSLException {
        this(config, traffic, channelTraffic, buildSslContext(config), buildQuicContext(config), buildDohSslContext(config));
    }

    public void addSslHandler(Channel channel) {
        SslHandler sslHandler = newSslHandler(channel);
        if (sslHandler != null) {
            channel.pipeline().addLast(sslHandler);
        }
    }

    public SslHandler newDohSslHandler(Channel channel, InetSocketAddress address) {
        if (dohSslContext == null) {
            throw new IllegalStateException("required DoH security setting not present");
        }
        return dohSslContext.newHandler(channel.alloc(), dohServerName(), address.getPort());
    }

    public ChannelFuture quicEndpoint(EventLoopGroup group) {
        QuicClientCodecBuilder builder = new QuicClientCodecBuilder()
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(0xfffff)
            .initialMaxStreamDataBidirectionalLocal(0xfffff);
        String serverName = Optional.ofNullable(config.getSsl()).map(SslSetting::getServerName).orElse(null);
        if (serverName != null) {
            builder.sslEngineProvider(c -> quicContext.newEngine(c.alloc(), serverName, config.getPort()));
        } else {
            builder.sslContext(quicContext);
        }
        return new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(builder.build()).bind(0);
    }

    private SslHandler newSslHandler(Channel channel) {
        if (sslContext == null) {
            return null;
        }
        String serverName = Optional.ofNullable(config.getSsl()).map(SslSetting::getServerName).orElse(config.getHost());
        SslHandler sslHandler = sslContext.newHandler(channel.alloc(), serverName, config.getPort());
        sslHandler.setCloseNotifyReadTimeout(1, TimeUnit.SECONDS);
        return sslHandler;
    }

    private String dohServerName() {
        return Optional.of(config).map(ServerConfig::getDns).map(DnsSetting::ssl).map(SslSetting::getServerName).orElse(null);
    }

    private static SslContext buildSslContext(ServerConfig config) throws SSLException {
        SslSetting sslSetting = config == null ? null : config.getSsl();
        if (sslSetting == null) {
            if (config != null && Protocol.trojan == config.getProtocol()) {
                throw new IllegalArgumentException("required security setting not present");
            }
            return null;
        }
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        if (sslSetting.getCertificateFile() != null) {
            sslContextBuilder.trustManager(new File(sslSetting.getCertificateFile()));
        }
        return sslContextBuilder.build();
    }

    private static QuicSslContext buildQuicContext(ServerConfig config) {
        if (config == null || !config.quicEnabled()) {
            return null; // keep quic native library untouched unless quic is really in use
        }
        SslSetting sslSetting = config.getSsl();
        if (sslSetting == null) {
            throw new IllegalArgumentException("required security setting not present");
        }
        QuicSslContextBuilder quicContextBuilder = QuicSslContextBuilder.forClient()
            .keylog(true)
            .applicationProtocols(ApplicationProtocolNames.HTTP_1_1);
        if (sslSetting.getCertificateFile() != null) {
            quicContextBuilder.trustManager(new File(sslSetting.getCertificateFile()));
        }
        return quicContextBuilder.build();
    }

    private static SslContext buildDohSslContext(ServerConfig config) throws SSLException {
        if (config == null || config.getDns() == null) {
            return null;
        }
        SslSetting sslSetting = config.getDns().ssl();
        return Doh.buildSslContext(sslSetting == null ? null : sslSetting.getCertificateFile());
    }
}
