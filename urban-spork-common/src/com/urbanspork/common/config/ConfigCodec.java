package com.urbanspork.common.config;

import java.io.IOException;

interface ConfigCodec {

    String encode(ClientConfig config) throws IOException;

    ClientConfig decode(String config) throws IOException;
}