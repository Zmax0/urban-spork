package com.urbanspork.common.config.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class ShareableServerConfig {
    private static final String SS = "ss";

    private ShareableServerConfig() {}

    public static Optional<ServerConfig> fromUri(URI uri) {
        String scheme = uri.getScheme();
        if (!SS.equals(scheme)) {
            return Optional.empty();
        }
        String userInfo = uri.getUserInfo();
        if (userInfo == null) {
            return Optional.empty();
        }
        int index = userInfo.indexOf(":");
        if (index == -1) {
            return Optional.empty();
        }
        String method = userInfo.substring(0, index);
        String password = userInfo.substring(index + 1);
        Optional<CipherKind> kind = CipherKind.from(method);
        if (kind.isEmpty()) {
            return Optional.empty();
        }
        ServerConfig config = new ServerConfig();
        config.setCipher(kind.get());
        config.setHost(uri.getHost());
        config.setPort(uri.getPort());
        config.setPassword(password);
        config.setRemark(uri.getFragment());
        config.setProtocol(Protocol.shadowsocks);
        return Optional.of(config);
    }

    public static Optional<URI> produceUri(ServerConfig config) {
        try {
            return Optional.of(new URI(SS, config.getCipher().toString() + ":" + config.getPassword(), config.getHost(), config.getPort(), null, null, config.getRemark()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
