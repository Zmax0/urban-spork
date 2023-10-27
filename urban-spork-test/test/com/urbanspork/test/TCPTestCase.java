package com.urbanspork.test;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TCPTestTemplate;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("TCP")
class TCPTestCase extends TCPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        ClientConfig config = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = config.getServers().get(0);
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.password());
        ExecutorService service = Executors.newFixedThreadPool(2);
        DefaultEventLoop eventLoop = new DefaultEventLoop();
        launchServer(service, eventLoop, config.getServers());
        launchClient(service, eventLoop, config);
        handshakeAndSendBytes(config);
        service.shutdownNow();
        eventLoop.shutdownGracefully();
    }
}
