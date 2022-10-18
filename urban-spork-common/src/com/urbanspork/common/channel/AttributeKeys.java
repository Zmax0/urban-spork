package com.urbanspork.common.channel;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksKey;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class AttributeKeys {

    public static final AttributeKey<ShadowsocksCipher> CIPHER = AttributeKey.newInstance("CIPHER");
    public static final AttributeKey<ShadowsocksKey> KEY = AttributeKey.newInstance("KEY");
    public static final AttributeKey<InetSocketAddress> SERVER_ADDRESS = AttributeKey.newInstance("SERVER_ADDRESS");

    private AttributeKeys() {

    }
}
