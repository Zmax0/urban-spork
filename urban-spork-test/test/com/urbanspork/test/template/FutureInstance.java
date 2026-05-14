package com.urbanspork.test.template;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public record FutureInstance<T>(Future<?> future, T instance) {
    @Override
    public String toString() {
        return "F" + System.identityHashCode(future) + "I" + System.identityHashCode(instance);
    }

    public void close(Consumer<T> consumer) {
        consumer.accept(instance);
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
