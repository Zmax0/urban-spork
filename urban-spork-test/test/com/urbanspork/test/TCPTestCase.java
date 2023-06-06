package com.urbanspork.test;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.test.template.TCPTestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@DisplayName("TCP")
class TCPTestCase extends TCPTestTemplate {

    @ParameterizedTest
    @ArgumentsSource(ParamProvider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        ClientConfig config = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = config.getServers().get(0);
        serverConfig.setProtocol(parameter.protocol);
        serverConfig.setCipher(parameter.cipher);
        ExecutorService service = Executors.newFixedThreadPool(2);
        launchServer(service, config.getServers());
        launchClient(service, config);
        handshakeAndSendBytes(config);
        service.shutdownNow();
    }

    private static class ParamProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            List<Parameter> parameters = new ArrayList<>();
            for (Protocols protocol : Protocols.values()) {
                for (SupportedCipher cipher : SupportedCipher.values()) {
                    parameters.add(new Parameter(protocol, cipher));
                }
            }
            return parameters.stream().map(Arguments::of);
        }
    }

    private record Parameter(Protocols protocol, SupportedCipher cipher) {
        @Override
        public String toString() {
            return String.format("%s|%s", protocol, cipher);
        }
    }
}
