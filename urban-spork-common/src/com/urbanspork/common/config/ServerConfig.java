package com.urbanspork.common.config;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;

public class ServerConfig {

    private String host;

    private String port;

    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
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
        StringBuilder builder = new StringBuilder(listItemText());
        if (protocol != null) {
            builder.append('|').append(protocol).append('|').append(cipher.toString());
        }
        if (builder.length() == 0) {
            builder.append(" XX ");
        }
        return builder.toString();
    }

    public String listItemText() {
        return remark == null || remark.isEmpty() ? host + ':' + port : remark;
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }

}
