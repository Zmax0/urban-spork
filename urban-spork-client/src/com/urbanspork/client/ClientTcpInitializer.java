package com.urbanspork.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

class ClientTcpInitializer extends ChannelInitializer<NioSocketChannel> {

    private volatile ClientChannelContext context;

    ClientTcpInitializer(ClientChannelContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        ClientChannelContext context = this.context;
        channel.pipeline().addLast(context.traffic(), new ClientProxyUnificationHandler(context));
    }

    void reloadContext(ClientChannelContext context) {
        this.context = context;
    }
}
