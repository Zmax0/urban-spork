package com.urbanspork.common.config;

import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class ServerConfigTestCase {

    @Test
    void testCheck() {
        ServerConfig config = testConfig(TestDice.randomPort());
        Assertions.assertTrue(config.check());
        config.setPassword(null);
        Assertions.assertFalse(config.check());
    }

    @Test
    void testUDPEnable() {
        ServerConfig config = testConfig(TestDice.randomPort());
        Assertions.assertTrue(config.udpEnabled());
        config.setNetworks(null);
        Assertions.assertFalse(config.udpEnabled());
    }

    public static ServerConfig testConfig(int port) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setHost("localhost");
        serverConfig.setPort(port);
        serverConfig.setProtocol(Protocols.shadowsocks);
        serverConfig.setCipher(TestDice.randomCipher());
        serverConfig.setPassword(UUID.randomUUID().toString());
        serverConfig.setNetworks(new Network[]{Network.TCP, Network.UDP});
        serverConfig.setPacketEncoding(PacketEncoding.Packet);
        return serverConfig;
    }
}
