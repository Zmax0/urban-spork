package com.urbanspork.test.interop;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class VMessInteroperabilityIntegrationTest extends InteropTestSupport {
    private static final String IMAGE = InteropPeerImages.load().vmess().reference();

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
        for (CipherKind cipher : new CipherKind[]{CipherKind.aes_128_gcm, CipherKind.chacha20_poly1305}) {
            for (var role : InteropCase.Role.values()) {
                for (var carrier : new InteropCase.Carrier[]{InteropCase.Carrier.TCP, InteropCase.Carrier.TLS, InteropCase.Carrier.WS, InteropCase.Carrier.WSS}) {
                    for (var traffic : InteropCase.Traffic.values()) {
                        cases.add(new InteropCase(role, cipher, carrier, traffic));
                    }
                }
            }
        }
        return cases.build();
    }

    @Override
    Protocol protocol() {
        return Protocol.vmess;
    }

    @Override
    ReferencePeer peer(InteropCase scenario, ServerConfig remote, int socksPort) {
        boolean client = scenario.role() == InteropCase.Role.SERVER;
        Map<String, Object> user = Map.of("id", remote.getPassword(), "alterId", 0, "security", scenario.cipher().toString());
        var stream = new LinkedHashMap<String, Object>();
        stream.put("network", scenario.websocket() ? "ws" : "tcp");
        stream.put("security", scenario.tls() ? "tls" : "none");
        if (scenario.websocket()) {
            stream.put("wsSettings", Map.of("path", "/interop"));
        }
        if (scenario.tls()) {
            var certificate = new LinkedHashMap<String, Object>();
            certificate.put("certificateFile", "/interop/localhost.crt");
            certificate.put("usage", client ? "verify" : "encipherment");
            if (!client) {
                certificate.put("keyFile", "/interop/localhost.key");
            }
            stream.put("tlsSettings", Map.of("serverName", "localhost", "allowInsecure", false, "certificates", List.of(certificate)));
        }
        var vmess = new LinkedHashMap<String, Object>();
        vmess.put("protocol", "vmess");
        vmess.put("streamSettings", stream);
        Map<String, Object> config;
        if (client) {
            vmess.put("settings", Map.of("vnext", List.of(Map.of("address", HOST, "port", remote.getPort(), "users", List.of(user)))));
            vmess.put("mux", Map.of("enabled", false));
            config = Map.of("log", Map.of("loglevel", "debug"),
                "inbounds", List.of(Map.of("listen", HOST, "port", socksPort, "protocol", "socks", "settings", Map.of("auth", "noauth", "udp", true, "ip", HOST))),
                "outbounds", List.of(vmess));
        } else {
            vmess.put("listen", HOST);
            vmess.put("port", remote.getPort());
            vmess.put("settings", Map.of("clients", List.of(user)));
            config = Map.of("log", Map.of("loglevel", "debug"), "inbounds", List.of(vmess), "outbounds", List.of(Map.of("protocol", "freedom")));
        }
        return new ReferencePeer(IMAGE, "v2ray", new String[]{"run", "-c", "/interop/config.json"}, new String[]{"version"}, config);
    }
}
