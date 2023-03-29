package com.urbanspork.common.config;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;

public class ServerConfig {

    private String host;

    private String port;

    private byte[] password;

    private SupportedCipher cipher;

    private Protocols protocol;

    private String remark;

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

    public SupportedCipher getCipher() {
        return cipher;
    }

    public void setCipher(SupportedCipher cipher) {
        this.cipher = cipher;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public Protocols getProtocol() {
        return protocol;
    }

    public ServerConfig setProtocol(Protocols protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean check() {
        return isEmpty(host) && isEmpty(port) && isEmpty(password) && cipher != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (remark != null) {
            builder.append(remark);
        }
        if (host != null) {
            builder.append(' ').append(host).append(':').append(port);
        }
        if (protocol != null) {
            builder.append(" [").append(protocol).append(']');
        }
        if (builder.length() == 0) {
            builder.append(" XX ");
        }
        return builder.toString();
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private boolean isEmpty(byte[] bytes) {
        return bytes != null && bytes.length > 0;
    }
}
