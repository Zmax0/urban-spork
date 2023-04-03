package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Socks5CommandRequest request;

    private final Channel localChannel;

    public ShadowsocksChannelInitializer(Socks5CommandRequest request, Channel localChannel) {
        this.request = request;
        this.localChannel = localChannel;
    }

    @Override
    public void initChannel(SocketChannel remoteChannel) {
        SupportedCipher cipher = localChannel.attr(AttributeKeys.CIPHER).get();
        String password = localChannel.attr(AttributeKeys.PASSWORD).get();
        remoteChannel.pipeline()
                .addLast(ShadowsocksAEADCipherCodecs.get(cipher, password))
                .addLast(new ShadowsocksAddressEncoder(request));
    }
}
