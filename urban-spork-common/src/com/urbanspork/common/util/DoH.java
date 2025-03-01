package com.urbanspork.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.urbanspork.common.protocol.dns.DoHRecord;
import com.urbanspork.common.protocol.dns.DoHResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
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
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DoH {
    private static final Logger logger = LoggerFactory.getLogger(DoH.class);

    public static Promise<String> lookup(EventLoopGroup group, String nameServer, String domain) throws InterruptedException {
        URI uri0 = URI.create(nameServer);
        if (uri0.getQuery() == null) {
            uri0 = URI.create(nameServer + "?name=" + domain);
        } else {
            uri0 = URI.create(nameServer + domain);
        }
        final URI uri = uri0;
        boolean isHttps = uri.getScheme().equals("https");
        int port0 = uri.getPort();
        if (port0 == -1) {
            port0 = isHttps ? 443 : 80;
        }
        final int port = port0;
        String host = uri.getHost();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString());
        request.headers().set(HttpHeaderNames.ACCEPT, "application/dns-json").set(HttpHeaderNames.HOST, host);
        Promise<String> promise = group.next().newPromise();
        new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws SSLException {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(
                        SslContextBuilder.forClient().build().newHandler(ch.alloc(), host, port),
                        new HttpClientCodec(),
                        new HttpObjectAggregator(0xffff),
                        new SimpleChannelInboundHandler<FullHttpResponse>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                                logger.debug("received doh response: {}", msg);
                                JsonMapper mapper = JsonMapper.builder()
                                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                    .build();
                                DoHResponse response;
                                try {
                                    response = mapper.readValue(msg.content().toString(StandardCharsets.UTF_8), DoHResponse.class);
                                } catch (JsonProcessingException e) {
                                    promise.setFailure(e);
                                    ctx.close();
                                    return;
                                }
                                response.getAnswer().stream().filter(a -> a.getType() == 1).findFirst().map(DoHRecord::getData).ifPresentOrElse(
                                    promise::setSuccess, () -> promise.setFailure(new IllegalStateException("No fitting doh answer found"))
                                );
                                ctx.close();
                            }
                        }
                    );
                }
            })
            .connect(host, port).sync()
            .channel().writeAndFlush(request).sync();
        return promise;
    }
}
