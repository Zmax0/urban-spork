package com.urbanspork.test;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.*;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TCPTestTemplate;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("TCP")
class TCPTestCase extends TCPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (Protocols.shadowsocks == protocol && cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        DefaultEventLoop executor = new DefaultEventLoop();
        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
        int[] ports = TestUtil.freePorts(2);
        ClientConfig config = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        launchServer(service, executor, config.getServers());
        launchClient(service, executor, config);
        handshakeAndSendBytes(config);
        service.shutdown();
        executor.shutdownGracefully();
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(ports[1]);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
        DefaultEventLoop executor = new DefaultEventLoop();
        launchServer(service, executor, List.of(serverConfig));
        ClientConfig config = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig current = config.getCurrent();
        current.setCipher(cipher);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        launchClient(service, executor, config);
        handshakeAndSendBytes(config);
        ServerUserManager.DEFAULT.clear();
        service.shutdown();
        executor.shutdownGracefully();
    }
}
