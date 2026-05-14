package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;

public class ConfigHandlerTest {
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
        ConfigHolder configHolder = new ConfigHolder() {
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
        };
        runWithCustomHolder(
            configHolder, () -> {
                ClientConfig config = ClientConfigTest.testConfig(0, 0);
                Assertions.assertThrows(UncheckedIOException.class, () -> ConfigHandler.DEFAULT.save(config));
                Assertions.assertThrows(UncheckedIOException.class, ConfigHandler.DEFAULT::read);
            }
        );
    }

    public static void runWithCustomJson(String json, Runnable runnable) throws NoSuchFieldException, IllegalAccessException {
        ConfigHolder configHolder = new ConfigHolder() {
            String temp = json;

            @Override
            public void save(String str) {
                temp = str;
            }

            @Override
            public String read() {
                return temp;
            }

            @Override
            public void delete() {
                temp = "";
            }
        };
        runWithCustomHolder(configHolder, runnable);
    }

    static void runWithCustomHolder(ConfigHolder configHolder, Runnable runnable) throws NoSuchFieldException, IllegalAccessException {
        Field holderField = ConfigHandler.class.getDeclaredField("holder");
        holderField.setAccessible(true);
        Object temp = holderField.get(ConfigHandler.DEFAULT);
        holderField.set(ConfigHandler.DEFAULT, configHolder);
        try {
            runnable.run();
        } finally {
            holderField.set(ConfigHandler.DEFAULT, temp);
        }
    }
}
