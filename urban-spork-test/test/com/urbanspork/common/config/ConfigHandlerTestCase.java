package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Common - Config Handler")
class ConfigHandlerTestCase {

    @Test
    void testSaveAndRead() {
        int clientPort = TestDice.rollPort();
        int serverPort = TestDice.rollPort();
        ConfigHandler.DEFAULT.save(ClientConfigTestCase.testConfig(clientPort, serverPort));
        ClientConfig config = ConfigHandler.DEFAULT.read();
        Assertions.assertEquals(clientPort, config.getPort());
        Assertions.assertEquals(serverPort, config.getServers().get(0).getPort());
    }

    @Test
    void testDelete() {
        ConfigHandler.DEFAULT.delete();
        Assertions.assertThrows(IllegalArgumentException.class, ConfigHandler.DEFAULT::read);
    }
}
