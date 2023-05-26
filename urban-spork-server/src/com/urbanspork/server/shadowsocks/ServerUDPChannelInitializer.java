package com.urbanspork.server.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
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
        pipeline.addLast(new ShadowsocksUDPReplayCodec(config))
            .addLast(new ServerUDPReplayHandler(config));
    }
}
