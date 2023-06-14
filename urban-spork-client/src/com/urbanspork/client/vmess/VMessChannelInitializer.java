package com.urbanspork.client.vmess;

import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class VMessChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Socks5CommandRequest request;

    private final ServerConfig config;

    public VMessChannelInitializer(Socks5CommandRequest request, ServerConfig config) {
        this.request = request;
        this.config = config;
    }

    @Override
    public void initChannel(SocketChannel remoteChannel) {
        remoteChannel.pipeline().addLast(new ClientAEADCodec(config.getCipher(), request, config.getPassword()));
    }

}
