package com.urbanspork.config;

import java.util.Objects;

import com.urbanspork.cipher.ShadowsocksCiphers;

public class ServerConfig {

    private String host;

    private String port;

    private String password;

    private ShadowsocksCiphers cipher;

    private String memo;

    public ServerConfig() {

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public ShadowsocksCiphers getCipher() {
        return cipher;
    }

    public void setCipher(ShadowsocksCiphers cipher) {
        this.cipher = cipher;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public ServerConfig checkSelf() {
        requireNonEmpty(host, "Host must not be null");
        requireNonEmpty(port, "Port must not be null");
        requireNonEmpty(password, "Password must not be null");
        Objects.requireNonNull(cipher, "Cipher must not be null");
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (memo != null) {
            builder.append('[').append(memo).append(']').append(' ');
        }
        if (host != null) {
            builder.append(host).append(':').append(port);
        }
        return builder.toString();
    }

    private void requireNonEmpty(String s, String msg) {
        if (s == null || s.isEmpty()) {
            throw new NullPointerException(msg);
        }
    }
}
