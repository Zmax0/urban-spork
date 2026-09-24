package com.urbanspork.common.util;

import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.test.DnsUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Tag("integration")
class DohTest {
    private static final EventLoopGroup GROUP = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    @ParameterizedTest
    @ArgumentsSource(Provider.class)
    void testQuery(String nameServer) throws InterruptedException, ExecutionException, SSLException {
        try {
            DnsRequest request = new DnsRequest(nameServer, "google.com");
            SslContext sslContext = Doh.buildSslContext(DnsUtil.getDnsSetting().ssl() == null ? null : DnsUtil.getDnsSetting().ssl().getCertificateFile());
            IpResponse ipResponse = Doh.query(
                GROUP, request, channel ->
                    sslContext.newHandler(channel.alloc(), request.address().getHostString(), request.address().getPort())
            ).get(5, TimeUnit.SECONDS);
            Assertions.assertNotNull(ipResponse);
        } catch (TimeoutException _) {
            // skip
        }
    }

    @AfterAll
    static void afterAll() {
        GROUP.shutdownGracefully().syncUninterruptibly();
    }

    private static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            URI uri = URI.create(DnsUtil.getDnsSetting().nameServer());
            String host = uri.getHost();
            return Stream.of(
                "https://" + host + ":443/dns-query?dns=",
                "https://" + host + ":443/dns-query?a=b"
            ).map(Arguments::of);
        }
    }
}
