package com.urbanspork.test.template;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record Parameter(Protocol protocol, CipherKind cipher, String clientPassword, String serverPassword) {
    @Override
    public String toString() {
        return String.format("%s|%s", protocol, cipher);
    }

    public static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            List<Parameter> parameters = new ArrayList<>();
            for (Protocol protocol : Protocol.values()) {
                if (protocol != Protocol.trojan) {
                    for (CipherKind cipher : CipherKind.values()) {
                        if (Protocol.vmess == protocol && cipher.isAead2022()) {
                            continue;
                        }
                        parameters.add(new Parameter(protocol, cipher, TestDice.rollPassword(protocol, cipher), TestDice.rollPassword(protocol, cipher)));
                    }
                }
            }
            parameters.add(new Parameter(Protocol.trojan, null, TestDice.rollString(), TestDice.rollString()));
            return parameters.stream().map(Arguments::of);
        }
    }
}
