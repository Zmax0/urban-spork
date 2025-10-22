package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class DohTestServer {
    public static final int PORT = 16803;
    private static final Logger logger = LoggerFactory.getLogger(DohTestServer.class);

    static void main() {
        launch(PORT, new DefaultPromise<>() {});
    }

    public static void launch(int port, Promise<ServerSocketChannel> promise) {
        URI crt;
        URI key;
        try {
            crt = Objects.requireNonNull(DohTestServer.class.getResource("/localhost.crt")).toURI();
            key = Objects.requireNonNull(DohTestServer.class.getResource("/localhost.key")).toURI();
        } catch (URISyntaxException e) {
            promise.setFailure(e);
            return;
        }
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder.forServer(new File(crt), new File(key), null)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1
                ))
                .build();
        } catch (SSLException e) {
            promise.setFailure(e);
            return;
        }
        try (EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
             EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())) {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new Http2ServerInitializer(sslContext))
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ServerSocketChannel channel = (ServerSocketChannel) future.channel();
                        logger.info("launch doh test server => {}", channel.localAddress());
                        promise.setSuccess(channel);
                    } else {
                        promise.setFailure(future.cause());
                    }
                }).sync().channel().closeFuture().sync();
            logger.info("Doh test server close");
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private static class Http2ServerInitializer extends ChannelInitializer<SocketChannel> {
        private final SslContext sslContext;

        public Http2ServerInitializer(SslContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        public void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(sslContext.newHandler(ch.alloc()), new Http2OrHttpHandler());
        }
    }

    private static class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
        private static final int MAX_CONTENT_LENGTH = 0xffff;

        protected Http2OrHttpHandler() {
            super(ApplicationProtocolNames.HTTP_1_1);
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                ctx.pipeline().addLast(
                    Http2FrameCodecBuilder.forServer().build(), new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            switch (msg) {
                                case Http2HeadersFrame headersFrame -> onHeadersRead(ctx, headersFrame);
                                case Http2DataFrame bodyFrame -> onDataRead(ctx, bodyFrame);
                                default -> super.channelRead(ctx, msg);
                            }
                        }

                        @Override
                        public void channelReadComplete(ChannelHandlerContext ctx) {
                            ctx.flush();
                        }

                        private static void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) {
                            Http2FrameStream stream = data.stream();
                            if (data.isEndStream()) {
                                sendResponse(ctx, stream, data.content());
                            } else {
                                data.release();
                            }
                            ctx.write(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(stream));
                        }

                        private static void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame frame) throws Exception {
                            if (frame.isEndStream()) {
                                ByteBuf content = ctx.alloc().ioBuffer(512);
                                String path = frame.headers().path().toString();
                                prepareResponse(path, content);
                                sendResponse(ctx, frame.stream(), content);
                            }
                        }

                        private static void sendResponse(ChannelHandlerContext ctx, Http2FrameStream stream, ByteBuf payload) {
                            Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
                            ctx.write(new DefaultHttp2HeadersFrame(headers).stream(stream));
                            ctx.write(new DefaultHttp2DataFrame(payload, true).stream(stream));
                        }
                    }
                );
                return;
            }
            if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                ctx.pipeline().addLast(
                    new HttpServerCodec(),
                    new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                    new SimpleChannelInboundHandler<FullHttpRequest>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(msg.protocolVersion(), OK);
                            prepareResponse(msg.uri(), response.content());
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                );
                return;
            }
            throw new IllegalStateException("unknown protocol: " + protocol);
        }

        private static void prepareResponse(String path, ByteBuf content) throws Exception {
            URI uri = new URI(path);
            Map<String, List<String>> parameters = new QueryStringDecoder(uri).parameters();
            Optional<String> dns = Optional.ofNullable(parameters.get("dns")).orElse(Collections.emptyList()).stream().findFirst();
            if (dns.isPresent()) {
                DnsQuery query = decode(Unpooled.wrappedBuffer(Base64.getUrlDecoder().decode(dns.get())));
                String resolved = Optional.ofNullable(parameters.get("resolved")).orElse(Collections.emptyList()).stream().findFirst().orElse("localhost");
                DnsRecord question = query.recordAt(DnsSection.QUESTION);
                DefaultDnsRawRecord answer = new DefaultDnsRawRecord(
                    question.name(),
                    question.type(),
                    60,
                    Unpooled.wrappedBuffer(NetUtil.createByteArrayFromIpAddressString(resolved))
                );
                DnsResponse dnsResponse = new DefaultDnsResponse(query.id(), query.opCode());
                dnsResponse.addRecord(DnsSection.QUESTION, question);
                dnsResponse.addRecord(DnsSection.ANSWER, answer);
                encode(dnsResponse, content);
            }
        }

        private static DnsQuery decode(ByteBuf buf) throws Exception {
            int id = buf.readUnsignedShort();
            int flags = buf.readUnsignedShort();
            if (flags >> 15 == 1) {
                throw new CorruptedFrameException("not a query");
            }
            DnsQuery query = new DefaultDnsQuery(id, DnsOpCode.valueOf((byte) (flags >> 11 & 0xf)));
            query.setRecursionDesired((flags >> 8 & 1) == 1);
            query.setZ(flags >> 4 & 0x7);
            int questionCount = buf.readUnsignedShort();
            int answerCount = buf.readUnsignedShort();
            int authorityRecordCount = buf.readUnsignedShort();
            int additionalRecordCount = buf.readUnsignedShort();
            decodeQuestions(query, buf, questionCount);
            decodeRecords(query, DnsSection.ANSWER, buf, answerCount);
            decodeRecords(query, DnsSection.AUTHORITY, buf, authorityRecordCount);
            decodeRecords(query, DnsSection.ADDITIONAL, buf, additionalRecordCount);
            return query;
        }

        private static void decodeQuestions(DnsQuery query, ByteBuf buf, int questionCount) throws Exception {
            for (int i = questionCount; i > 0; --i) {
                query.addRecord(DnsSection.QUESTION, DnsRecordDecoder.DEFAULT.decodeQuestion(buf));
            }
        }

        private static void decodeRecords(DnsQuery query, DnsSection section, ByteBuf buf, int count) throws Exception {
            for (int i = count; i > 0; --i) {
                DnsRecord r = DnsRecordDecoder.DEFAULT.decodeRecord(buf);
                if (r == null) {
                    break;
                }
                query.addRecord(section, r);
            }
        }

        private static void encode(DnsResponse response, ByteBuf buf) throws Exception {
            encodeHeader(response, buf);
            encodeQuestions(response, buf);
            encodeRecords(response, DnsSection.ANSWER, buf);
            encodeRecords(response, DnsSection.AUTHORITY, buf);
            encodeRecords(response, DnsSection.ADDITIONAL, buf);
        }

        private static void encodeHeader(DnsResponse response, ByteBuf buf) {
            buf.writeShort(response.id());
            int flags = 32768;
            flags |= (response.opCode().byteValue() & 0xFF) << 11;
            if (response.isAuthoritativeAnswer()) {
                flags |= 1 << 10;
            }
            if (response.isTruncated()) {
                flags |= 1 << 9;
            }
            if (response.isRecursionDesired()) {
                flags |= 1 << 8;
            }
            if (response.isRecursionAvailable()) {
                flags |= 1 << 7;
            }
            flags |= response.z() << 4;
            flags |= response.code().intValue();
            buf.writeShort(flags);
            buf.writeShort(response.count(DnsSection.QUESTION));
            buf.writeShort(response.count(DnsSection.ANSWER));
            buf.writeShort(response.count(DnsSection.AUTHORITY));
            buf.writeShort(response.count(DnsSection.ADDITIONAL));
        }

        private static void encodeQuestions(DnsResponse response, ByteBuf buf) throws Exception {
            int count = response.count(DnsSection.QUESTION);
            for (int i = 0; i < count; ++i) {
                DnsRecordEncoder.DEFAULT.encodeQuestion(response.recordAt(DnsSection.QUESTION, i), buf);
            }
        }

        private static void encodeRecords(DnsResponse response, DnsSection section, ByteBuf buf) throws Exception {
            int count = response.count(section);
            for (int i = 0; i < count; ++i) {
                DnsRecordEncoder.DEFAULT.encodeRecord(response.recordAt(section, i), buf);
            }
        }
    }
}
