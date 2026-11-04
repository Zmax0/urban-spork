package com.urbanspork.common.util;

import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.DnsResponseDecoder;
import com.urbanspork.common.protocol.dns.IpResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
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
import java.util.function.Function;

public class Doh {
    private static final Logger logger = LoggerFactory.getLogger(Doh.class);

    private Doh() {}

    public static SslContext buildSslContext(String certificateFile) throws SSLException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2,
                ApplicationProtocolNames.HTTP_1_1
            ));
        if (certificateFile != null) {
            sslContextBuilder.trustManager(new File(certificateFile));
        }
        return sslContextBuilder.build();
    }

    public static Promise<IpResponse> query(EventLoopGroup group, DnsRequest request, Function<Channel, SslHandler> sslHandlers) {
        Promise<IpResponse> promise = group.next().newPromise();
        new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelHandlerAdapter() {})
            .connect(request.address()).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    Channel channel = f.channel();
                    query(channel, request, promise, sslHandlers.apply(channel));
                } else {
                    promise.tryFailure(f.cause());
                }
            });
        return promise;
    }

    public static void query(Channel channel, DnsRequest request, Promise<IpResponse> promise, SslHandler ssl) {
        InetSocketAddress address = request.address();
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
                            DnsRecord dnsRecord = dnsResponse.recordAt(DnsSection.ANSWER, i);
                            if (dnsRecord.type() == DnsRecordType.A) {
                                DnsRawRecord rawRecord = (DnsRawRecord) dnsRecord;
                                String ip = NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(rawRecord.content()));
                                promise.setSuccess(new IpResponse(ip, dnsRecord.timeToLive()));
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
        logger.debug("send DoH request {} to {}", request, address);
    }
}
