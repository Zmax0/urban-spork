package com.urbanspork.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class ClientChannelTrafficHandler extends ChannelTrafficShapingHandler {
    private final String host;
    private final int port;
    private final ClientChannelContext context;
    private String channelId;

    public ClientChannelTrafficHandler(String host, int port, ClientChannelContext context) {
        super(0, 0, 1000);
        this.host = host;
        this.port = port;
        this.context = context;
    }

    public String getHost() {
        return host + ":" + port;
    }

    public long getDownloaded() {
        return trafficCounter.cumulativeReadBytes();
    }

    public long getUploaded() {
        return trafficCounter.cumulativeWrittenBytes();
    }

    public long getDlSpeed() {
        return trafficCounter.lastReadThroughput();
    }

    public long getUlSpeed() {
        return trafficCounter.lastWriteThroughput();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.channelId = ctx.channel().id().asShortText();
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        context.channelTraffic().remove(channelId);
        super.handlerRemoved(ctx);
    }

    @Override
    public boolean equals(Object o) {
        if (channelId != null && o instanceof ClientChannelTrafficHandler other) {
            return channelId.equals(other.channelId);
        } else {
            return this == o;
        }
    }

    @Override
    public int hashCode() {
        if (channelId != null) {
            return channelId.hashCode();
        } else {
            return super.hashCode();
        }
    }
}
