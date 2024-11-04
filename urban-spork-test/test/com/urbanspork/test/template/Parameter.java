package com.urbanspork.test.template;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record Parameter(Protocol protocol, CipherKind cipher, String clientPassword, String serverPassword, WebSocketSetting wsSetting, SslSetting sslSetting) {
    @Override
    public String toString() {
        if (wsSetting != null) {
            if (sslSetting != null) {
                return String.format("%s|%s|wss", protocol, cipher);
            } else {
                return String.format("%s|%s|ws", protocol, cipher);
            }
        } else {
            if (sslSetting != null) {
                return String.format("%s|%s|ssl", protocol, cipher);
            } else {
                return String.format("%s|%s", protocol, cipher);
            }
        }
    }

    public static class Provider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            SslSetting sslSetting = SslUtil.getSslSetting();
            WebSocketSetting wsSetting = new WebSocketSetting();
            wsSetting.setPath("/ws");
            List<Parameter> parameters = new ArrayList<>();
            for (Protocol protocol : Protocol.values()) {
                if (protocol != Protocol.trojan) {
                    for (CipherKind cipher : CipherKind.values()) {
                        if (Protocol.vmess == protocol && cipher.isAead2022()) {
                            continue;
                        }
                        String clientPassword = TestDice.rollPassword(protocol, cipher);
                        String serverPassword = TestDice.rollPassword(protocol, cipher);
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, null, null));
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, null, sslSetting));
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, wsSetting, null));
                        parameters.add(new Parameter(protocol, cipher, clientPassword, serverPassword, wsSetting, sslSetting));
                    }
                }
            }
            parameters.add(new Parameter(Protocol.trojan, null, TestDice.rollString(), TestDice.rollString(), null, sslSetting));
            parameters.add(new Parameter(Protocol.trojan, null, TestDice.rollString(), TestDice.rollString(), wsSetting, sslSetting));
            return parameters.stream().map(Arguments::of);
        }
    }
}
