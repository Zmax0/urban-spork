package com.urbanspork.test.template;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record Parameter(Protocols protocol, SupportedCipher cipher) {
    @Override
    public String toString() {
        return String.format("%s|%s", protocol, cipher);
    }

    public static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            List<Parameter> parameters = new ArrayList<>();
            for (Protocols protocol : Protocols.values()) {
                for (SupportedCipher cipher : SupportedCipher.values()) {
                    parameters.add(new Parameter(protocol, cipher));
                }
            }
            return parameters.stream().map(Arguments::of);
        }
    }
}
