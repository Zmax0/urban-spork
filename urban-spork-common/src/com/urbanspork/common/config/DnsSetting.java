package com.urbanspork.common.config;

public class DnsSetting {
    private String nameServer;
    private SslSetting ssl;

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public SslSetting getSsl() {
        return ssl;
    }

    public void setSsl(SslSetting ssl) {
        this.ssl = ssl;
    }
}
