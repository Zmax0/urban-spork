package com.urbanspork.test.template;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TraceLevelLoggerTestTemplate {

    private final Logger logger = logger();
    private Level former;

    @BeforeAll
    void beforeAll() {
        former = logger.getEffectiveLevel();
        logger.setLevel(Level.TRACE);
    }

    @AfterAll
    void afterAll() {
        logger.setLevel(former);
    }

    protected abstract Logger logger();
}
