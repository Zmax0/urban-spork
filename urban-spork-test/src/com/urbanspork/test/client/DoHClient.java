package com.urbanspork.test.client;

import com.urbanspork.common.util.DoH;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DoHClient {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Promise<String> promise = DoH.lookup(group, "https://1.1.1.1/dns-query", "www.example.com");
        System.out.println(promise.get(10, TimeUnit.SECONDS));
        group.shutdownGracefully();
    }
}
