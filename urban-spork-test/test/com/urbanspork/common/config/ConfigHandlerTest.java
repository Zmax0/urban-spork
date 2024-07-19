package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;

class ConfigHandlerTest {
    @Test
    void testSaveAndRead() {
        int clientPort = TestDice.rollPort();
        int serverPort = TestDice.rollPort();
        ConfigHandler.DEFAULT.save(ClientConfigTest.testConfig(clientPort, serverPort));
        ConfigHandler.DEFAULT.save(ConfigHandler.DEFAULT.read());
        ClientConfig config = ConfigHandler.DEFAULT.read();
        Assertions.assertEquals(clientPort, config.getPort());
        Assertions.assertEquals(serverPort, config.getServers().getFirst().getPort());
        Assertions.assertDoesNotThrow(ConfigHandler.DEFAULT::delete);
    }

    @Test
    void testException() throws NoSuchFieldException, IllegalAccessException {
        Field holderField = ConfigHandler.class.getDeclaredField("holder");
        holderField.setAccessible(true);
        Object temp = holderField.get(ConfigHandler.DEFAULT);
        holderField.set(ConfigHandler.DEFAULT, new ConfigHolder() {
            @Override
            public void save(String str) throws IOException {
                throw new IOException();
            }

            @Override
            public String read() throws IOException {
                throw new IOException();
            }

            @Override
            public void delete() throws IOException {
                throw new IOException();
            }
        });
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        Assertions.assertThrows(UncheckedIOException.class, () -> ConfigHandler.DEFAULT.save(config));
        Assertions.assertThrows(UncheckedIOException.class, ConfigHandler.DEFAULT::read);
        holderField.set(ConfigHandler.DEFAULT, temp);
    }
}
