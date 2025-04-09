package com.urbanspork.common.util;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DohTest {
    private final static NioEventLoopGroup GROUP = new NioEventLoopGroup();

    @ParameterizedTest
    @ValueSource(strings = {
        "https://dns.google/resolve",
        "https://dns.google:443/resolve?name=",
    })
    public void testQuery(String nameServer) throws InterruptedException, ExecutionException {
        try {
            Doh.query(GROUP, nameServer, "www.example.com").get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // skip
        }
    }

    @AfterAll
    public static void afterAll() {
        GROUP.shutdownGracefully();
    }
}
