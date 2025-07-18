package com.urbanspork.common.util;

import com.urbanspork.test.DnsUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

class DohTest {
    private final static EventLoopGroup GROUP = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    @ParameterizedTest
    @ArgumentsSource(Provider.class)
    public void testQuery(String nameServer) throws InterruptedException, ExecutionException {
        try {
            Doh.query(GROUP, nameServer, "example.com").get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // skip
        }
    }

    @AfterAll
    public static void afterAll() {
        GROUP.shutdownGracefully();
    }

    private static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            String nameServer = DnsUtil.getDnsSetting().getNameServer();
            URI uri = URI.create(nameServer);
            String host = uri.getHost();
            return Stream.of(
                "https://" + host + "/dns-query",
                "https://" + host + ":443/dns-query?dns=",
                "https://" + host + ":443/dns-query?a=b"
            ).map(Arguments::of);
        }
    }
}
