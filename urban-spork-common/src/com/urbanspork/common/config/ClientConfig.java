package com.urbanspork.common.config;

import java.util.List;

public class ClientConfig {

    private String host;

    private int port = 1080;

    private String language;

    private int index;

    private List<ServerConfig> servers;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers;
    }

    public ServerConfig getCurrent() {
        ServerConfig current = null;
        List<ServerConfig> s = getServers();
        if (s != null && !s.isEmpty()) {
            current = s.get(Math.min(getIndex(), s.size() - 1));
        }
        return current;
    }

    @Override
    public String toString() {
        return String.format("localhost:%s - %s", getPort(), getCurrent().clientText());
    }
}
