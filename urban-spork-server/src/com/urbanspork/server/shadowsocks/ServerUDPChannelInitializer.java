package com.urbanspork.server.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class ServerUDPChannelInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig config;

    public ServerUDPChannelInitializer(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new ShadowsocksUDPReplayCodec(ShadowsocksAEADCipherCodecs.get(config.getPassword(), config.getCipher(), Network.UDP)))
                .addLast(new ServerUDPReplayHandler(config));
    }
}
