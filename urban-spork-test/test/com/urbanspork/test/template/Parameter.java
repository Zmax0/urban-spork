package com.urbanspork.test.template;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public record Parameter(Protocol protocol, CipherKind cipher, String clientPassword, String serverPassword, Transport[] transport) {
    @Override
    public String toString() {
        if (Protocol.trojan != protocol && transport != null && Arrays.stream(transport).anyMatch(t -> t == Transport.WS)) {
            return String.format("%s|%s|ws", protocol, cipher);
        } else {
            return String.format("%s|%s", protocol, cipher);
        }
    }

    public static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            Transport[] normal = new Transport[]{Transport.UDP, Transport.TCP};
            Transport[] websocket = new Transport[]{Transport.WS, Transport.UDP};
            List<Parameter> parameters = new ArrayList<>();
            for (Protocol protocol : Protocol.values()) {
                if (protocol != Protocol.trojan) {
                    for (CipherKind cipher : CipherKind.values()) {
                        if (Protocol.vmess == protocol && cipher.isAead2022()) {
                            continue;
                        }
                        String clientPassword = TestDice.rollPassword(protocol, cipher);
                        String serverPassword = TestDice.rollPassword(protocol, cipher);
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, normal));
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, websocket));
                    }
                }
            }
            parameters.add(new Parameter(Protocol.trojan, null, TestDice.rollString(), TestDice.rollString(), null));
            return parameters.stream().map(Arguments::of);
        }
    }
}
