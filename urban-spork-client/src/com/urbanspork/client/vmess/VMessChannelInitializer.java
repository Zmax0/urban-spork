package com.urbanspork.client.vmess;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.SupportedCipher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class VMessChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Socks5CommandRequest request;

    private final Channel localChannel;

    public VMessChannelInitializer(Socks5CommandRequest request, Channel localChannel) {
        this.request = request;
        this.localChannel = localChannel;
    }

    @Override
    public void initChannel(SocketChannel remoteChannel) {
        String uuid = localChannel.attr(AttributeKeys.PASSWORD).get();
        SupportedCipher cipher = localChannel.attr(AttributeKeys.CIPHER).get();
        remoteChannel.pipeline().addLast(ClientCodecs.get(uuid, request, cipher));
    }

}
