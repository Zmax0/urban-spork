package com.urbanspork.common.util;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DoHTest {
    private final static NioEventLoopGroup GROUP = new NioEventLoopGroup();

    @ParameterizedTest
    @ValueSource(strings = {
        "https://1.1.1.1/dns-query",
        "https://1.1.1.1:443/dns-query?name=",
    })
    public void testLookup(String nameServer) throws InterruptedException, ExecutionException {
        try {
            DoH.lookup(GROUP, nameServer, "www.example.com").get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // skip
        }
    }

    @AfterAll
    public static void afterAll() {
        GROUP.shutdownGracefully();
    }
}
