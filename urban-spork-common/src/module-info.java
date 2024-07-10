module com.urbanspork.common {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec.socks;
    requires io.netty.codec;
    requires io.netty.common;
    requires io.netty.handler;
    requires io.netty.transport;
    requires jdk.unsupported;
    requires org.bouncycastle.lts.prov;
    requires org.slf4j;

    exports com.urbanspork.common.channel;
    exports com.urbanspork.common.codec.aead;
    exports com.urbanspork.common.codec.chunk;
    exports com.urbanspork.common.codec.shadowsocks.tcp;
    exports com.urbanspork.common.codec.shadowsocks.udp;
    exports com.urbanspork.common.codec.shadowsocks;
    exports com.urbanspork.common.codec.socks;
    exports com.urbanspork.common.codec.vmess;
    exports com.urbanspork.common.codec;
    exports com.urbanspork.common.config.shadowsocks;
    exports com.urbanspork.common.config;
    exports com.urbanspork.common.crypto;
    exports com.urbanspork.common.lang;
    exports com.urbanspork.common.manage.shadowsocks;
    exports com.urbanspork.common.protocol.socks;
    exports com.urbanspork.common.protocol.trojan;
    exports com.urbanspork.common.protocol.vmess.aead;
    exports com.urbanspork.common.protocol.vmess.encoding;
    exports com.urbanspork.common.protocol.vmess.header;
    exports com.urbanspork.common.protocol.vmess;
    exports com.urbanspork.common.protocol;
    exports com.urbanspork.common.transport.tcp;
    exports com.urbanspork.common.transport.udp;
    exports com.urbanspork.common.transport;
    exports com.urbanspork.common.util;
}