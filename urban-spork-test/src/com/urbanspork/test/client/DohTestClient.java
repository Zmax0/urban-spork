package com.urbanspork.test.client;

import com.urbanspork.common.config.DnsSetting;
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
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;

import javax.net.ssl.SSLException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DohTestClient {
    static void main() throws InterruptedException, ExecutionException, TimeoutException, SSLException {
        SslSetting sslSetting = new SslSetting();
        ClassLoader classLoader = DohTestClient.class.getClassLoader();
        sslSetting.setCertificateFile(Objects.requireNonNull(classLoader.getResource("localhost.crt")).getFile());
        sslSetting.setKeyFile(Objects.requireNonNull(classLoader.getResource("localhost.key")).getFile());
        sslSetting.setServerName("localhost");
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        DnsSetting dnsSetting = new DnsSetting("localhost", sslSetting);
        DnsRequest quest = new DnsRequest("https://localhost:" + DohTestServer.PORT + "/dns-query", ".example.com");
        SslContext sslContext = Doh.buildSslContext(dnsSetting.ssl().getCertificateFile());
        Promise<IpResponse> promise = group.next().newPromise();
        Channel channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
            .handler(new ChannelHandlerAdapter() {})
            .connect(quest.address()).sync()
            .channel();
        Doh.query(channel, quest, promise, sslContext.newHandler(channel.alloc(), dnsSetting.ssl().getServerName(), quest.address().getPort()));
        System.out.println(promise.get(10, TimeUnit.SECONDS));
        group.shutdownGracefully();
    }
}
