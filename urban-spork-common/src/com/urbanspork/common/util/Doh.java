package com.urbanspork.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.DohRecord;
import com.urbanspork.common.protocol.dns.DohResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

public class Doh {
    private static final Logger logger = LoggerFactory.getLogger(Doh.class);

    public static Promise<String> query(EventLoopGroup group, String nameServer, String domain) throws InterruptedException {
        DnsRequest<FullHttpRequest> quest = getRequest(nameServer, domain, null);
        Promise<String> promise = group.next().newPromise();
        Channel channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelHandlerAdapter() {})
            .connect(quest.address()).sync()
            .channel();
        query(channel, quest, promise);
        return promise;
    }

    public static void query(Channel channel, DnsRequest<FullHttpRequest> request, Promise<String> promise) {
        SslHandler ssl;
        try {
            InetSocketAddress address = request.address();
            String serverName = address.getHostString();
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            SslSetting sslSetting = request.ssl();
            if (sslSetting != null) {
                if (sslSetting.getCertificateFile() != null) {
                    sslContextBuilder.trustManager(new File(sslSetting.getCertificateFile()));
                }
                if (sslSetting.getServerName() != null) {
                    serverName = sslSetting.getServerName(); // override
                }
            }
            ssl = sslContextBuilder.build().newHandler(channel.alloc(), serverName, address.getPort());
        } catch (SSLException e) {
            promise.setFailure(e);
            return;
        }
        HttpClientCodec httpClient = new HttpClientCodec();
        HttpObjectAggregator httpAggregator = new HttpObjectAggregator(0xffff);
        channel.pipeline().addLast(
            ssl, httpClient, httpAggregator,
            new SimpleChannelInboundHandler<FullHttpResponse>(false) {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                    logger.debug("received doh response: {}", msg);
                    ChannelPipeline pipeline = ctx.pipeline();
                    pipeline.remove(ssl);
                    pipeline.remove(httpClient);
                    pipeline.remove(httpAggregator);
                    pipeline.remove(this);
                    JsonMapper mapper = JsonMapper.builder()
                        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();
                    DohResponse response;
                    try {
                        response = mapper.readValue(msg.content().toString(StandardCharsets.UTF_8), DohResponse.class);
                    } catch (JsonProcessingException e) {
                        promise.setFailure(e);
                        return;
                    }
                    Optional.ofNullable(response.getAnswer()).orElse(Collections.emptyList()).stream().filter(a -> a.getType() == 1).findFirst().map(DohRecord::getData).ifPresentOrElse(
                        promise::setSuccess, () -> promise.setFailure(new IllegalStateException("no fitting doh answer found"))
                    );
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.setFailure(cause);
                }
            }
        );
        channel.writeAndFlush(request.msg());
    }

    public static DnsRequest<FullHttpRequest> getRequest(String nameServer, String domain, SslSetting ssl) {
        URI uri = URI.create(nameServer);
        if (uri.getQuery() == null) {
            uri = URI.create(nameServer + "?name=" + domain);
        } else {
            uri = URI.create(nameServer + domain);
        }
        int port = uri.getPort();
        if (port == -1) {
            port = uri.getScheme().equals("https") ? 443 : 80;
        }
        String host = uri.getHost();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString());
        request.headers().set(HttpHeaderNames.ACCEPT, "application/dns-json").set(HttpHeaderNames.HOST, host);
        return new DnsRequest<>(InetSocketAddress.createUnresolved(host, port), ssl, request);
    }
}