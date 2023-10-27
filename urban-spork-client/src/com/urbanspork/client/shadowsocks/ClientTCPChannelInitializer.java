package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.TCPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
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
        channel.pipeline().addLast(new TCPReplayCodec(StreamType.Request, request, config.getCipher(), config.getPassword()));
    }
}
