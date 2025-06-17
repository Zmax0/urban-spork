package com.urbanspork.test.client;

import com.urbanspork.common.util.Doh;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DohClient {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            Promise<String> promise = Doh.query(group, "https://8.8.8.8/dns-query", "example.com");
            String result = promise.get(10, TimeUnit.SECONDS);
            System.out.println(result);
        } finally {
            group.shutdownGracefully();
        }
    }
}
