package com.urbanspork.client.config;

import java.util.List;

public class ClientConfig {

    private static final String DEFAULT_PORT = "1081";

    private String port;

    private ServerConfig current;

    private List<ServerConfig> servers;

    public String getPort() {
        return port == null ? DEFAULT_PORT : port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public ServerConfig getCurrent() {
        return current;
    }

    public void setCurrent(ServerConfig current) {
        this.current = current;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getCurrent()).append(' ').append('@').append("localhost:").append(getPort());
        return builder.toString();
    }

}
