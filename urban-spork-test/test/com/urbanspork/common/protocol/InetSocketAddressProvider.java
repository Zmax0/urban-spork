package com.urbanspork.common.protocol;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.InetSocketAddress;
import java.util.stream.Stream;

public class InetSocketAddressProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        return Stream.of(
            InetSocketAddress.createUnresolved("www.example.com", 80),
            InetSocketAddress.createUnresolved("www.example.com", 443),
            new InetSocketAddress(0),
            new InetSocketAddress("192.168.89.9", TestDice.rollPort()),
            new InetSocketAddress("abcd:ef01:2345:6789:abcd:ef01:2345:6789", TestDice.rollPort())
        ).map(Arguments::of);
    }
}