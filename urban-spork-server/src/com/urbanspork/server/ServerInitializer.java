package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressDecoder;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.server.vmess.ServerAEADCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class ServerInitializer extends ChannelInitializer<Channel> {

    private final ServerConfig config;

    public ServerInitializer(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(Channel c) {
        ChannelPipeline pipeline = c.pipeline();
        if (Protocols.vmess == config.getProtocol()) {
            pipeline.addLast(new ServerAEADCodec(new String[]{config.getPassword()}));
        } else {
            pipeline.addLast(ShadowsocksAEADCipherCodecs.get(config.getPassword(), config.getCipher(), Network.TCP))
                .addLast(new ShadowsocksAddressDecoder());
        }
        pipeline.addLast(new RemoteConnectionHandler());
    }

}
