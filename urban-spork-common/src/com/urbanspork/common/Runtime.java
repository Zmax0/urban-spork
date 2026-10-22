package com.urbanspork.common;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;

public record Runtime(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
    public Runtime() {
        this(
            new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("parent"), NioIoHandler.newFactory()),
            new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("child"), NioIoHandler.newFactory())
        );
    }

    public void close() {
        Future<?> shutdownChildGroup = childGroup.shutdownGracefully();
        parentGroup.shutdownGracefully().syncUninterruptibly();
        shutdownChildGroup.syncUninterruptibly();
    }
}
