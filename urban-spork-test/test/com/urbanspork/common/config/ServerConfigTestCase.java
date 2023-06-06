package com.urbanspork.common.config;

import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DisplayName("Common - Server Config")
public class ServerConfigTestCase {

    @Test
    void testCheck() {
        ServerConfig config = testConfig(TestDice.randomPort());
        Assertions.assertTrue(config.check());
        config.setCipher(null);
        Assertions.assertFalse(config.check());
        config.setPassword("");
        Assertions.assertFalse(config.check());
        config.setHost("");
        Assertions.assertFalse(config.check());
    }

    @Test
    void testUDPEnable() {
        ServerConfig config = testConfig(TestDice.randomPort());
        Assertions.assertTrue(config.udpEnabled());
        config.setNetworks(null);
        Assertions.assertFalse(config.udpEnabled());
    }

    @Test
    void testToString() {
        ServerConfig config = testConfig(TestDice.randomPort());
        String string = config.toString();
        Assertions.assertTrue(string.contains(config.getProtocol().toString()));
        Assertions.assertTrue(string.contains(config.getCipher().toString()));
    }

    @Test
    void testListItemText() {
        ServerConfig config = testConfig(TestDice.randomPort());
        String string = config.listItemText();
        Assertions.assertTrue(string.contains(config.getHost()));
        Assertions.assertTrue(string.contains(String.valueOf(config.getPort())));
    }

    public static ServerConfig testConfig(int port) {
        return testConfig(new int[]{port}).get(0);
    }

    public static List<ServerConfig> testConfig(int[] ports) {
        List<ServerConfig> serverConfigs = new ArrayList<>(ports.length);
        for (int port : ports) {
            ServerConfig serverConfig = new ServerConfig();
            serverConfig.setHost("localhost");
            serverConfig.setPort(port);
            serverConfig.setProtocol(Protocols.shadowsocks);
            serverConfig.setCipher(TestDice.randomCipher());
            serverConfig.setPassword(UUID.randomUUID().toString());
            serverConfig.setNetworks(new Network[]{Network.TCP, Network.UDP});
            serverConfig.setPacketEncoding(PacketEncoding.Packet);
            serverConfigs.add(serverConfig);
        }
        return serverConfigs;
    }
}
