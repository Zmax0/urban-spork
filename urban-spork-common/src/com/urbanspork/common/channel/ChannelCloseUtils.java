package com.urbanspork.common.channel;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public final class ChannelCloseUtils {

    private ChannelCloseUtils() {}

    public static void closeOnFlush(Channel ch) {
        ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
