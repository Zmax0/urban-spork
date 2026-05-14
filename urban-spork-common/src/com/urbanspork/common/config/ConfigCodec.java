package com.urbanspork.common.config;

interface ConfigCodec {

    String encode(ClientConfig config);

    ClientConfig decode(String config);
}