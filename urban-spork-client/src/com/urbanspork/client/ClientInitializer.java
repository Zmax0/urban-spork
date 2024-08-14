package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final ClientInitializationContext context;

    ClientInitializer(ClientInitializationContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) {
        channel.attr(AttributeKeys.SERVER_CONFIG).set(context.config().getCurrent());
        channel.pipeline().addLast(context.traffic(), new ClientProxyUnificationHandler());
    }
}
