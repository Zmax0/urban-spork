package com.urbanspork.common.config;

import java.io.IOException;

public enum ConfigHandler {

    DEFAULT(new JSONConfigCodec(), new FileConfigHolder());

    private final ConfigCodec codec;
    private final ConfigHolder holder;

    ConfigHandler(ConfigCodec codec, ConfigHolder holder) {
        this.codec = codec;
        this.holder = holder;
    }

    public ClientConfig read() throws IOException {
        return codec.decode(holder.read());
    }

    public void write(ClientConfig config) throws IOException {
        holder.write(codec.encode(config));
    }
}
