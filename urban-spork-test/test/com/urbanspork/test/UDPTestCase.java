package com.urbanspork.test;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.*;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UDPTestTemplate;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("UDP")
class UDPTestCase extends UDPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        CipherKind cipher = parameter.cipher();
        Protocols protocol = parameter.protocol();
        Network[] networks = {Network.TCP, Network.UDP};
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(ports[1]);
        serverConfig.setNetworks(networks);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        if (Protocols.shadowsocks == protocol && cipher.isAead2022() && cipher.supportEih()) {
            List<ServerUserConfig> user = new ArrayList<>();
            user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
            serverConfig.setUser(user);
        }
        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
        DefaultEventLoop executor = new DefaultEventLoop();
        launchServer(service, executor, List.of(serverConfig));
        ClientConfig clientConfig = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(cipher);
        current.setNetworks(networks);
        current.setProtocol(protocol);
        if (Protocols.shadowsocks == protocol && cipher.isAead2022() && cipher.supportEih()) {
            current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        } else {
            current.setPassword(parameter.serverPassword());
        }
        launchClient(service, executor, clientConfig);
        for (int dstPort : dstPorts()) {
            handshakeAndSendBytes(clientConfig, dstPort);
        }
        service.shutdown();
        executor.shutdownGracefully();
    }
}
