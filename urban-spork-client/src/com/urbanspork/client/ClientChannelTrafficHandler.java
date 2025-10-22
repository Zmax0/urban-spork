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
        channelId = ctx.channel().id().asShortText();
        context.channelTraffic().putIfAbsent(channelId, this);
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        context.channelTraffic().remove(channelId);
        super.handlerRemoved(ctx);
    }
}
