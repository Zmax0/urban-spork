package com.urbanspork.client.shadowsocks;

import com.urbanspork.client.ClientPromiseHandler;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.shadowsocks.impl.ShadowsocksCipherCodecs;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksProtocolEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.concurrent.Promise;

public class ShadowsocksChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Socks5CommandRequest request;

    private final Channel localChannel;

    private final Promise<Channel> promise;

    public ShadowsocksChannelInitializer(Socks5CommandRequest request, Channel localChannel, Promise<Channel> promise) {
        this.request = request;
        this.localChannel = localChannel;
        this.promise = promise;
    }

    @Override
    public void initChannel(SocketChannel remoteChannel) {
        SupportedCipher cipher = localChannel.attr(AttributeKeys.CIPHER).get();
        byte[] password = localChannel.attr(AttributeKeys.PASSWORD).get();
        remoteChannel.pipeline()
                .addLast(ShadowsocksCipherCodecs.get(cipher, password))
                .addLast(new ShadowsocksProtocolEncoder(request))
                .addLast(new ClientPromiseHandler(promise));
    }
}
