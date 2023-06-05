package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Common - Config Handler")
class ConfigHandlerTestCase {
    @Test
    void testSaveAndRead() {
        int clientPort = TestDice.randomPort();
        int serverPort = TestDice.randomPort();
        TestUtil.testConfig(clientPort, serverPort).save();
        ClientConfig config = ConfigHandler.DEFAULT.read();
        Assertions.assertEquals(clientPort, config.getPort());
        Assertions.assertEquals(serverPort, config.getServers().get(0).getPort());
    }
}
