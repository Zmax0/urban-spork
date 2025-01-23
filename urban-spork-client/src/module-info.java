module com.urbanspork.client {
    exports com.urbanspork.client;
    requires com.urbanspork.common;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec.socks;
    requires io.netty.codec;
    requires io.netty.common;
    requires io.netty.handler;
    requires io.netty.incubator.codec.classes.quic;
    requires io.netty.transport;
    requires org.slf4j;
}