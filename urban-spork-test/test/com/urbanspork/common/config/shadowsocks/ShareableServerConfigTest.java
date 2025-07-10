package com.urbanspork.common.config.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

class ShareableServerConfigTest {
    @Test
    void testFromAndProduce() {
        URI uri = URI.create("ss://2022-blake3-aes-128-gcm:zCjw%2FijIimRXjECgtogjd7IsgvPL7cdyqU5J8BIfobM=:Y57CGhfZ%2Fmln4tBzJ4J78AMNLbJoakz8T+5v0SaEkfI=@example.com:443#name");
        Optional<ServerConfig> op = ShareableServerConfig.fromUri(uri);
        Assertions.assertTrue(op.isPresent());
        Optional<URI> op2 = ShareableServerConfig.produceUri(op.get());
        Assertions.assertTrue(op2.isPresent());
        Assertions.assertEquals(op2.get(), uri);
    }

    @Test
    void testProduceIllegalUri() {
        ServerConfig config = new ServerConfig();
        config.setCipher(CipherKind.aes_128_gcm);
        Assertions.assertFalse(ShareableServerConfig.produceUri(config).isPresent());
    }

    @ParameterizedTest
    @ArgumentsSource(IllegalUriProvider.class)
    void testFromIllegalUri(URI uri) {
        Assertions.assertFalse(ShareableServerConfig.fromUri(uri).isPresent());
    }

    static class IllegalUriProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext extensionContext) {
            return Stream.of(
                "file://2022-blake3-aes-128-gcm:5mOQSa20Kt6ay2LXruBoHQ%3D%3D@example.com:443/#name",
                "ss://unknown:5mOQSa20Kt6ay2LXruBoHQ%3D%3D@example.com:443/#name",
                "ss://unknown@example.com:443/#name",
                "ss://example.com:443/#name"
            ).map(URI::create).map(Arguments::of);
        }
    }
}