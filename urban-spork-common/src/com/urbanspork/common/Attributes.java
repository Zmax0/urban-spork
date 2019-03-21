package com.urbanspork.common;

import java.net.InetSocketAddress;

import javax.crypto.SecretKey;

import com.urbanspork.cipher.ShadowsocksCipher;

import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;

public class Attributes {

    public static final AttributeKey<ShadowsocksCipher> CIPHER = AttributeKey.newInstance("CIPHER");
    public static final AttributeKey<SecretKey> KEY = AttributeKey.newInstance("KEY");
    public static final AttributeKey<InetSocketAddress> SERVER_ADDRESS = AttributeKey.newInstance("SERVER_ADDRESS");
    public static final AttributeKey<InetSocketAddress> REMOTE_ADDRESS = AttributeKey.newInstance("REMOTE_ADDRESS");
    public static final AttributeKey<Socks5CommandRequest> REQUEST = AttributeKey.newInstance("REQUEST");

}
