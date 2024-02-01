package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.TCPRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ClientTCPChannelInitializer extends ChannelInitializer<Channel> {

    private final Socks5CommandRequest request;

    private final ServerConfig config;

    public ClientTCPChannelInitializer(Socks5CommandRequest request, ServerConfig config) {
        this.request = request;
        this.config = config;
    }

    @Override
    public void initChannel(Channel channel) {
        channel.pipeline().addLast(new TCPRelayCodec(Mode.Client, request, config));
    }
}
