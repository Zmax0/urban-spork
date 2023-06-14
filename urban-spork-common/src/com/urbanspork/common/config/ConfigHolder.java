package com.urbanspork.common.config;

import java.io.IOException;

interface ConfigHolder {

    void save(String str) throws IOException;

    String read() throws IOException;

    void delete() throws IOException;
}
