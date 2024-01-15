package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

@DisplayName("Common - Config Handler")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigHandlerTestCase {

    @Test
    @Order(1)
    void testSaveAndRead() {
        int clientPort = TestDice.rollPort();
        int serverPort = TestDice.rollPort();
        ConfigHandler.DEFAULT.save(ClientConfigTestCase.testConfig(clientPort, serverPort));
        ConfigHandler.DEFAULT.save(ConfigHandler.DEFAULT.read());
        ClientConfig config = ConfigHandler.DEFAULT.read();
        Assertions.assertEquals(clientPort, config.getPort());
        Assertions.assertEquals(serverPort, config.getServers().getFirst().getPort());
    }

    @Test
    @Order(2)
    void testException() throws IOException {
        ClientConfig config = ConfigHandler.DEFAULT.read();
        File file = FileConfigHolder.path().toFile();
        Assertions.assertTrue(file.setWritable(false));
        Assertions.assertThrows(UncheckedIOException.class, () -> ConfigHandler.DEFAULT.save(config));
        Assertions.assertTrue(file.setWritable(true));
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            lines = reader.lines().collect(Collectors.toList());
        }
        lines.removeFirst();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
            }
        }
        Assertions.assertThrows(UncheckedIOException.class, ConfigHandler.DEFAULT::read);
        ConfigHandler.DEFAULT.delete();
        Assertions.assertThrows(IllegalArgumentException.class, ConfigHandler.DEFAULT::read);
    }
}
