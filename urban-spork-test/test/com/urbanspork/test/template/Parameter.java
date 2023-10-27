package com.urbanspork.test.template;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record Parameter(Protocols protocol, CipherKind cipher, String password) {
    @Override
    public String toString() {
        return String.format("%s|%s", protocol, cipher);
    }

    public static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            List<Parameter> parameters = new ArrayList<>();
            for (Protocols protocol : Protocols.values()) {
                for (CipherKind cipher : CipherKind.values()) {
                    parameters.add(new Parameter(protocol, cipher, TestDice.rollPassword(protocol, cipher)));
                }
            }
            return parameters.stream().map(Arguments::of);
        }
    }
}
