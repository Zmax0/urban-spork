package com.urbanspork.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final ClientChannelContext context;

    ClientInitializer(ClientChannelContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        channel.pipeline().addLast(context.traffic(), new ClientProxyUnificationHandler(context));
    }
}
