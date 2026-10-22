package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.ToIntFunction;

abstract class TestTemplate {
    protected static final Logger logger = LoggerFactory.getLogger(TestTemplate.class);
    protected static final Runtime CLIENT_RUNTIME = new Runtime();
    protected static final Runtime SERVER_RUNTIME = new Runtime();
    protected static final int SERVER_PORT = getPortOrDefault("com.urbanspork.test.server.port", Integer::parseInt);
    protected static final int CLIENT_PORT = getPortOrDefault("com.urbanspork.test.client.port", Integer::parseInt);
    private static DohTestServer DOH_TEST_SERVER;

    protected static Client.Instance launchClient(ClientConfig config) throws InterruptedException, ExecutionException {
        Client.Instance instance = Client.launch(CLIENT_RUNTIME, config).get();
        logger.info("new client: {}", instance);
        return instance;
    }

    protected static Server.Instances launchServer(List<ServerConfig> configs) throws InterruptedException {
        Server.Instances instances = Server.launch(SERVER_RUNTIME, configs);
        logger.info("new server: {}", instances);
        return instances;
    }

    private static int getPortOrDefault(String key, ToIntFunction<String> converter) {
        String property = System.getProperty(key);
        return property == null ? 0 : converter.applyAsInt(property);
    }

    protected static ClientConfig testConfig() {
        return ClientConfigTest.testConfig(CLIENT_PORT, SERVER_PORT);
    }

    protected static DohTestServer dohTestServer() throws ExecutionException, InterruptedException {
        if (DOH_TEST_SERVER == null || !DOH_TEST_SERVER.isActive()) {
            try {
                DOH_TEST_SERVER = DohTestServer.launch(0);
            } catch (SSLException e) {
                throw new ExecutionException("launch doh test server failed", e);
            }
        }
        return DOH_TEST_SERVER;
    }

    protected static void updateConfig(ServerConfig config, Parameter parameter) {
        config.setProtocol(parameter.protocol());
        config.setCipher(parameter.cipher());
        config.setPassword(parameter.serverPassword());
        config.setSsl(parameter.sslSetting());
        config.setWs(parameter.wsSetting());
    }
}
