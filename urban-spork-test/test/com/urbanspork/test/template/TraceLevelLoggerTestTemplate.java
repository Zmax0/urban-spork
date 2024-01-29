package com.urbanspork.test.template;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TraceLevelLoggerTestTemplate {

    private final Logger logger = logger();
    private Level former;

    private Logger logger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(loggerClass());
    }

    @BeforeAll
    void beforeAll() {
        former = logger.getEffectiveLevel();
        logger.setLevel(Level.TRACE);
    }

    @AfterAll
    void afterAll() {
        logger.setLevel(former);
    }

    protected abstract Class<?> loggerClass();
}
