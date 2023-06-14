package com.urbanspork.common.channel;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.util.Map;

public final class ChannelCloseUtils {

    private ChannelCloseUtils() {}

    public static void closeOnFlush(Channel ch) {
        ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public static void clearMap(Map<?, Channel> map) {
        for (Map.Entry<?, Channel> entry : map.entrySet()) {
            entry.getValue().close();
        }
        map.clear();
    }
}
