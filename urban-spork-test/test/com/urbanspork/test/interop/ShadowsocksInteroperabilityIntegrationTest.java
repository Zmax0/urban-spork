package com.urbanspork.test.interop;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

class ShadowsocksInteroperabilityIntegrationTest extends InteropTestSupport {
    private static final InteropPeerImages IMAGES = InteropPeerImages.load();

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void forwards(InteropCase scenario) throws Exception {
        verify(scenario, false);
    }

    @ParameterizedTest(name = "{0}-aes-128-gcm-tcp-tcp-invalid-credentials")
    @EnumSource(InteropCase.Role.class)
    void rejectsInvalidCredentials(InteropCase.Role role) throws Exception {
        verify(new InteropCase(role, CipherKind.aes_128_gcm, InteropCase.Carrier.TCP, InteropCase.Traffic.TCP), true);
    }

    static Stream<InteropCase> scenarios() {
        var cases = Stream.<InteropCase>builder();
        for (CipherKind cipher : new CipherKind[]{CipherKind.aes_128_gcm, CipherKind.aes_256_gcm, CipherKind.chacha20_poly1305,
            CipherKind.aead2022_blake3_aes_128_gcm, CipherKind.aead2022_blake3_aes_256_gcm, CipherKind.aead2022_blake3_chacha20_poly1305}) {
            for (var role : InteropCase.Role.values()) {
                for (var traffic : InteropCase.Traffic.values()) {
                    var carrier = traffic == InteropCase.Traffic.UDP ? InteropCase.Carrier.UDP : InteropCase.Carrier.TCP;
                    cases.add(new InteropCase(role, cipher, carrier, traffic));
                }
            }
        }
        return cases.build();
    }

    @Override
    Protocol protocol() {
        return Protocol.shadowsocks;
    }

    @Override
    ReferencePeer peer(InteropCase scenario, ServerConfig remote, int socksPort) {
        var config = new LinkedHashMap<String, Object>();
        config.put("server", HOST);
        config.put("server_port", remote.getPort());
        config.put("password", remote.getPassword());
        config.put("method", scenario.cipher() == CipherKind.chacha20_poly1305 ? "chacha20-ietf-poly1305" : scenario.cipher().toString());
        config.put("mode", "tcp_and_udp");
        boolean client = scenario.role() == InteropCase.Role.SERVER;
        if (client) {
            config.put("local_address", HOST);
            config.put("local_port", socksPort);
        }
        return new ReferencePeer((client ? IMAGES.shadowsocksClient() : IMAGES.shadowsocksServer()).reference(), client ? "sslocal" : "ssserver",
            new String[]{"-v", "-c", "/interop/config.json"}, new String[]{"--version"}, config);
    }
}
