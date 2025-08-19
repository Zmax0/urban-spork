package com.urbanspork.common.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.function.Consumer;

public interface FutureListeners {
    static <V> GenericFutureListener<Future<V>> failure(Consumer<Throwable> consumer) {
        return f -> {
            if (!f.isSuccess()) {
                consumer.accept(f.cause());
            }
        };
    }

    static <V> GenericFutureListener<Future<V>> success(Consumer<V> consumer) {
        return f -> {
            if (f.isSuccess()) {
                consumer.accept(f.get());
            }
        };
    }
}
