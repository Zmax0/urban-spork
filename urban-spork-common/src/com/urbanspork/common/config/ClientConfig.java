package com.urbanspork.common.config;

import java.io.IOException;
import java.util.List;

public class ClientConfig {

    private static final String DEFAULT_PORT = "1081";

    private String port;

    private String language;

    private int index;

    private List<ServerConfig> servers;

    public String getPort() {
        return port == null ? DEFAULT_PORT : port;
    }

    public void setPort(String port) {
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
            int i = getIndex();
            if (i < s.size()) {
                current = s.get(i);
            }
        }
        return current;
    }

    public void save() throws IOException {
        ConfigHandler.write(this);
    }

    @Override
    public String toString() {
        return String.format("%s @ localhost:%s", getCurrent(), getPort());
    }

}
