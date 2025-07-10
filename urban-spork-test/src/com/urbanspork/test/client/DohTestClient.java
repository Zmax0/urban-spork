package com.urbanspork.test.client;

import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.util.Doh;
import com.urbanspork.test.server.tcp.DohTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.Promise;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DohTestClient {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        SslSetting sslSetting = new SslSetting();
        ClassLoader classLoader = DohTestClient.class.getClassLoader();
        sslSetting.setCertificateFile(Objects.requireNonNull(classLoader.getResource("localhost.crt")).getFile());
        sslSetting.setKeyFile(Objects.requireNonNull(classLoader.getResource("localhost.key")).getFile());
        sslSetting.setServerName("localhost");
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        DnsRequest<FullHttpRequest> quest = Doh.getRequest("https://localhost:" + DohTestServer.PORT + "/dns-query", ".example.com", sslSetting);
        Promise<IpResponse> promise = group.next().newPromise();
        Channel channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelHandlerAdapter() {})
            .connect(quest.address()).sync()
            .channel();
        Doh.query(channel, quest, promise);
        System.out.println(promise.get(10, TimeUnit.SECONDS));
        group.shutdownGracefully();
    }
}
