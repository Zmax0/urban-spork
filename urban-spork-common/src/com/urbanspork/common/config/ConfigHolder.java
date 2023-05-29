package com.urbanspork.common.config;

import java.io.IOException;

interface ConfigHolder {

    void write(String str) throws IOException;

    String read() throws IOException;
}
