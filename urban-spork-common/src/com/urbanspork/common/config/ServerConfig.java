package com.urbanspork.common.config;

import com.urbanspork.common.cipher.ShadowsocksCiphers;

public class ServerConfig {

    private String host;

    private String port;

    private String password;

    private ShadowsocksCiphers cipher;

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
        if (builder.length() == 0) {
            builder.append("未配置的服务器");
        }
        return builder.toString();
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
