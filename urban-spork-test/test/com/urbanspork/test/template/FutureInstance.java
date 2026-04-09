package com.urbanspork.test.template;

import java.util.concurrent.Future;

public record FutureInstance<T>(Future<?> future, T instance) {
    @Override
    public String toString() {
        return System.identityHashCode(future) + "-" + System.identityHashCode(instance);
    }
}
