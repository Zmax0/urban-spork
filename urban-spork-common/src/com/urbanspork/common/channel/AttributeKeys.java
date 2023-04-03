package com.urbanspork.common.channel;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class AttributeKeys {

    public static final AttributeKey<Protocols> PROTOCOL = AttributeKey.newInstance("PROTOCOL");
    public static final AttributeKey<SupportedCipher> CIPHER = AttributeKey.newInstance("CIPHER");
    public static final AttributeKey<InetSocketAddress> SERVER_ADDRESS = AttributeKey.newInstance("SERVER_ADDRESS");
    public static final AttributeKey<String> PASSWORD = AttributeKey.newInstance("PASSWORD");

    private AttributeKeys() {

    }
}
