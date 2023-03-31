package com.urbanspork.client.vmess;

import com.urbanspork.client.ClientPromiseHandler;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.SupportedCipher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.concurrent.Promise;

public class VMessChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Socks5CommandRequest request;

    private final Channel localChannel;

    private final Promise<Channel> promise;

    public VMessChannelInitializer(Socks5CommandRequest request, Channel localChannel, Promise<Channel> promise) {
        this.request = request;
        this.localChannel = localChannel;
        this.promise = promise;
    }

    @Override
    public void initChannel(SocketChannel remoteChannel) {
        String uuid = new String(localChannel.attr(AttributeKeys.PASSWORD).get());
        SupportedCipher cipher = localChannel.attr(AttributeKeys.CIPHER).get();
        remoteChannel.pipeline()
                .addLast(ClientCodecs.get(uuid, request, cipher))
                .addLast(new ClientPromiseHandler(promise));
    }
}
