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
        ServerConfig config = testConfig(TestDice.rollPort());
        Assertions.assertTrue(config.check());
        config.setCipher(null);
        Assertions.assertFalse(config.check());
        config.setPassword(null);
        Assertions.assertFalse(config.check());
        config.setHost("");
        Assertions.assertFalse(config.check());
    }

    @Test
    void testUDPEnable() {
        ServerConfig config = testConfig(TestDice.rollPort());
        config.setNetworks(new Network[]{Network.UDP});
        Assertions.assertTrue(config.udpEnabled());
        config.setNetworks(new Network[]{Network.TCP});
        Assertions.assertFalse(config.udpEnabled());
        config.setNetworks(null);
        Assertions.assertFalse(config.udpEnabled());
    }

    @Test
    void testToString() {
        ServerConfig config = testConfig(TestDice.rollPort());
        String string = config.toString();
        Assertions.assertTrue(string.contains(config.getProtocol().toString()));
        Assertions.assertTrue(string.contains(config.getCipher().toString()));
        config.setProtocol(null);
        Assertions.assertEquals(config.toString(), config.listItemText());
    }

    @Test
    void testListItemText() {
        ServerConfig config = testConfig(TestDice.rollPort());
        config.setRemark("");
        String text = config.listItemText();
        Assertions.assertTrue(text.contains(config.getHost()));
        Assertions.assertTrue(text.contains(String.valueOf(config.getPort())));
        String remark = TestDice.rollString();
        config.setRemark(remark);
        Assertions.assertEquals(remark, config.listItemText());
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
            serverConfig.setCipher(TestDice.rollCipher());
            serverConfig.setPassword(UUID.randomUUID().toString());
            serverConfig.setPacketEncoding(PacketEncoding.Packet);
            serverConfigs.add(serverConfig);
        }
        return serverConfigs;
    }
}
