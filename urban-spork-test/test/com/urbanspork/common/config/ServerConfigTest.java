package com.urbanspork.common.config;

import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.PacketEncoding;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ServerConfigTest {
    @Test
    void testUDPEnable() {
        ServerConfig config = testConfig(TestDice.rollPort());
        config.setTransport(new Transport[]{Transport.UDP});
        Assertions.assertTrue(config.udpEnabled());
        config.setTransport(new Transport[]{Transport.TCP});
        Assertions.assertFalse(config.udpEnabled());
        config.setTransport(null);
        Assertions.assertFalse(config.udpEnabled());
        config.setTransport(new Transport[]{});
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

    @Test
    void testNetworkText() {
        ServerConfig config = new ServerConfig();
        config.setTransport(new Transport[]{Transport.UDP});
        config.setProtocol(Protocol.shadowsocks);
        Assertions.assertEquals("udp", config.networkText());
        config.setTransport(new Transport[]{Transport.QUIC});
        Assertions.assertEquals("quic", config.networkText());
        config.setTransport(new Transport[]{Transport.TCP});
        Assertions.assertEquals("tcp", config.networkText());
        SslSetting ssl = SslUtil.getSslSetting();
        config.setSsl(ssl);
        Assertions.assertEquals("tls", config.networkText());
        config.setSsl(null);
        WebSocketSetting ws = new WebSocketSetting();
        config.setWs(ws);
        Assertions.assertEquals("ws", config.networkText());
        config.setSsl(ssl);
        Assertions.assertEquals("wss", config.networkText());
    }

    public static ServerConfig testConfig(int port) {
        return testConfigs(port).getFirst();
    }

    public static List<ServerConfig> testConfigs(int... ports) {
        List<ServerConfig> serverConfigs = new ArrayList<>(ports.length);
        for (int port : ports) {
            ServerConfig serverConfig = new ServerConfig();
            serverConfig.setHost(InetAddress.getLoopbackAddress().getHostName());
            serverConfig.setPort(port);
            serverConfig.setProtocol(Protocol.shadowsocks);
            serverConfig.setCipher(TestDice.rollCipher());
            serverConfig.setPassword(TestDice.rollPassword(serverConfig.getProtocol(), serverConfig.getCipher()));
            serverConfig.setPacketEncoding(PacketEncoding.Packet);
            serverConfigs.add(serverConfig);
        }
        return serverConfigs;
    }
}
