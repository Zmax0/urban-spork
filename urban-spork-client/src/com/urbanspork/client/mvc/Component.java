package com.urbanspork.client.mvc;

import java.util.Objects;

public enum Component {

    Controller(null), Console(null);

    private Object component;

    private Component(Object component) {
        this.component = component;
    }

    public void set(Object component) {
        this.component = Objects.requireNonNull(component);
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) this.component;
    }

}