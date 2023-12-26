package com.urbanspork.server.vmess;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.test.template.TCPTestTemplate;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag("dependent")
@DisplayName("VMess - TCP")
class VMessTCPTestCase extends TCPTestTemplate {
    @Test
    void testLocal() throws ExecutionException, InterruptedException {
        ClientConfig config = ConfigHandler.DEFAULT.read();
        List<ServerConfig> configs = config.getServers();
        Assertions.assertEquals(Protocols.vmess, configs.getFirst().getProtocol());
        ExecutorService service = Executors.newSingleThreadExecutor();
        DefaultEventLoop eventLoop = new DefaultEventLoop();
        launchServer(service, eventLoop, configs);
        handshakeAndSendBytes(config);
        service.shutdownNow();
        eventLoop.shutdownGracefully();
    }
}
