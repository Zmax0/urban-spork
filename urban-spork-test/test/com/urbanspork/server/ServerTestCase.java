package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.List;

@DisplayName("Server")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTestCase {
    @Test
    void launchServer() {
        List<ServerConfig> emptyConfigs = Collections.emptyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(emptyConfigs), "Server config in the file is empty");
        List<ServerConfig> configs = TestUtil.testConfig(TestDice.randomPort(), TestDice.randomPort()).getServers();
        ServerConfig config = configs.get(0);
        config.setHost("www.urban-spork.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(configs), "None available server");
    }
}
