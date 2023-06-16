package com.urbanspork.common.config;

import java.io.IOException;
import java.io.UncheckedIOException;

public enum ConfigHandler {

    DEFAULT(new JSONConfigCodec(), new FileConfigHolder());

    private final ConfigCodec codec;
    private final ConfigHolder holder;

    ConfigHandler(ConfigCodec codec, ConfigHolder holder) {
        this.codec = codec;
        this.holder = holder;
    }

    public ClientConfig read() {
        try {
            return codec.decode(holder.read());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void save(ClientConfig config) {
        try {
            holder.save(codec.encode(config));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete() {
        try {
            holder.delete();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
