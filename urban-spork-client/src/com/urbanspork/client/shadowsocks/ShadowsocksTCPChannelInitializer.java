package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressEncoder;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksTCPChannelInitializer extends ChannelInitializer<Channel> {

    private final Socks5CommandRequest request;

    private final ServerConfig config;

    public ShadowsocksTCPChannelInitializer(Socks5CommandRequest request, ServerConfig config) {
        this.request = request;
        this.config = config;
    }

    @Override
    public void initChannel(Channel channel) {
        channel.pipeline().addLast(
                ShadowsocksAEADCipherCodecs.get(config.getPassword(), config.getCipher(), Network.TCP),
                new ShadowsocksAddressEncoder(request)
        );
    }
}
