package com.urbanspork.test;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UDPTestTemplate;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("UDP")
class UDPTestCase extends UDPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        ClientConfig clientConfig = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = clientConfig.getServers().getFirst();
        serverConfig.setNetworks(new Network[]{Network.TCP, Network.UDP});
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.password());
        ExecutorService service = Executors.newFixedThreadPool(2);
        DefaultEventLoop executor = new DefaultEventLoop();
        launchClient(service, executor, clientConfig);
        launchServer(service, executor, clientConfig.getServers());
        for (int dstPort : dstPorts()) {
            handshakeAndSendBytes(clientConfig, dstPort);
        }
        service.shutdownNow();
        executor.shutdownGracefully();
    }
}
