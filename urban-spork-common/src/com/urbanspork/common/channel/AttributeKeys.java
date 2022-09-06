package com.urbanspork.common.channel;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksKey;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class AttributeKeys {

    public static final AttributeKey<ShadowsocksCipher> CIPHER = AttributeKey.newInstance("CIPHER");
    public static final AttributeKey<ShadowsocksKey> KEY = AttributeKey.newInstance("KEY");
    public static final AttributeKey<InetSocketAddress> SERVER_ADDRESS = AttributeKey.newInstance("SERVER_ADDRESS");
    public static final AttributeKey<InetSocketAddress> REMOTE_ADDRESS = AttributeKey.newInstance("REMOTE_ADDRESS");
    public static final AttributeKey<Socks5CommandRequest> REQUEST = AttributeKey.newInstance("REQUEST");
    public static final AttributeKey<EventLoopGroup> WORKER = AttributeKey.newInstance("WORKER");

    private AttributeKeys() {

    }
}
