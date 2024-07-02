package com.urbanspork.common.config;

import java.util.Map;

public class WebSocketSetting {
    private Map<String, String> header;
    private String path;

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
