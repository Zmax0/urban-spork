package com.urbanspork.test.interop;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class TrojanInteroperabilityIntegrationTest extends InteropTestSupport {
    private static final String IMAGE = InteropPeerImages.load().trojan().reference();

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void forwards(InteropCase scenario) throws Exception {
        verify(scenario, false);
    }

    @ParameterizedTest(name = "{0}-password-tls-tcp-invalid-credentials")
    @EnumSource(InteropCase.Role.class)
    void rejectsInvalidCredentials(InteropCase.Role role) throws Exception {
        verify(new InteropCase(role, null, InteropCase.Carrier.TLS, InteropCase.Traffic.TCP), true);
    }

    static Stream<InteropCase> scenarios() {
        var cases = Stream.<InteropCase>builder();
        for (var role : InteropCase.Role.values()) {
            for (var traffic : InteropCase.Traffic.values()) {
                cases.add(new InteropCase(role, null, InteropCase.Carrier.TLS, traffic));
            }
        }
        return cases.build();
    }

    @Override
    Protocol protocol() {
        return Protocol.trojan;
    }

    @Override
    ReferencePeer peer(InteropCase scenario, ServerConfig remote, int socksPort) {
        boolean client = scenario.role() == InteropCase.Role.SERVER;
        var ssl = new LinkedHashMap<String, Object>();
        ssl.put("cert", "/interop/localhost.crt");
        ssl.put("alpn", List.of("http/1.1"));
        if (client) {
            ssl.put("verify", true);
            ssl.put("verify_hostname", true);
            ssl.put("sni", "localhost");
        } else {
            ssl.put("key", "/interop/localhost.key");
        }
        Map<String, Object> config = Map.of("run_type", client ? "client" : "server",
            "local_addr", HOST, "local_port", client ? socksPort : remote.getPort(),
            "remote_addr", HOST, "remote_port", client ? remote.getPort() : 1,
            "password", List.of(remote.getPassword()), "log_level", 0, "ssl", ssl);
        return new ReferencePeer(IMAGE, "trojan", new String[]{"/interop/config.json"}, new String[]{"--version"}, config);
    }
}
