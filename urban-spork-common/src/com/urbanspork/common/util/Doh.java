package com.urbanspork.common.util;

import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.dns.DnsQueryEncoder;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.DnsResponseDecoder;
import com.urbanspork.common.protocol.dns.IpResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.DefaultDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Doh {
    private static final Logger logger = LoggerFactory.getLogger(Doh.class);

    public static Promise<IpResponse> query(EventLoopGroup group, String nameServer, String domain) throws InterruptedException {
        DnsRequest<FullHttpRequest> quest = getRequest(nameServer, domain, null);
        Promise<IpResponse> promise = group.next().newPromise();
        Channel channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelHandlerAdapter() {})
            .connect(quest.address()).sync()
            .channel();
        query(channel, quest, promise);
        return promise;
    }

    public static void query(Channel channel, DnsRequest<FullHttpRequest> request, Promise<IpResponse> promise) {
        SslHandler ssl;
        try {
            InetSocketAddress address = request.address();
            String serverName = address.getHostString();
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1
                ));
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
        Http2Connection connection = new DefaultHttp2Connection(false);
        HttpToHttp2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(new DelegatingDecompressorFrameListener(
                connection,
                new InboundHttp2ToHttpAdapterBuilder(connection).maxContentLength(0xffff).build(),
                0
            ))
            .connection(connection).build();
        channel.pipeline().addLast(
            ssl, connectionHandler,
            new SimpleChannelInboundHandler<FullHttpResponse>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                    logger.debug("received DoH response: {}", msg);
                    ChannelPipeline pipeline = ctx.pipeline();
                    pipeline.remove(ssl);
                    pipeline.remove(connectionHandler);
                    pipeline.remove(this);
                    DnsResponse dnsResponse = DnsResponseDecoder.decode(msg.content());
                    try {
                        if (dnsResponse.code() != DnsResponseCode.NOERROR) {
                            promise.setFailure(new IllegalStateException("DoH response code is " + dnsResponse.code()));
                            return;
                        }
                        for (int i = 0; i < dnsResponse.count(DnsSection.ANSWER); i++) {
                            DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                            if (record.type() == DnsRecordType.A) {
                                DnsRawRecord rawRecord = (DnsRawRecord) record;
                                String ip = NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(rawRecord.content()));
                                promise.setSuccess(new IpResponse(ip, record.timeToLive()));
                                return;
                            }
                        }
                        promise.setFailure(new IllegalStateException("No type-a answer found"));
                    } finally {
                        dnsResponse.release();
                    }
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
        short tid = (short) ThreadLocalRandom.current().nextInt(); // unsigned short
        DefaultDnsQuery dnsQuery = new DefaultDnsQuery(tid);
        DefaultDnsQuestion question = new DefaultDnsQuestion(domain, DnsRecordType.A);
        dnsQuery.addRecord(DnsSection.QUESTION, question);
        domain = Base64.getUrlEncoder().withoutPadding().encodeToString(DnsQueryEncoder.encode(dnsQuery));
        dnsQuery.release();
        URI uri = URI.create(nameServer);
        Map<String, List<String>> queryParams;
        if (uri.getQuery() == null) {
            uri = URI.create(nameServer + "?dns=" + domain);
        } else if ((queryParams = QueryStringDecoder.builder().build(uri).parameters()).containsKey("dns")) {
            List<String> dns = queryParams.get("dns");
            dns.clear();
            dns.add(domain);
            QueryStringEncoder encoder = new QueryStringEncoder(nameServer.replace("?" + uri.getQuery(), ""));
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                encoder.addParam(entry.getKey(), entry.getValue().getFirst());
            }
            uri = URI.create(encoder.toString());
        } else {
            uri = URI.create(nameServer + "&dns=" + domain);
        }
        int port = uri.getPort();
        if (port == -1) {
            port = uri.getScheme().equals("https") ? 443 : 80;
        }
        String host = uri.getHost();
        FullHttpRequest msg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString());
        msg.headers().set(HttpHeaderNames.ACCEPT, "application/dns-message").set(HttpHeaderNames.CONTENT_TYPE, "application/dns-message");
        return new DnsRequest<>(InetSocketAddress.createUnresolved(host, port), ssl, msg);
    }
}