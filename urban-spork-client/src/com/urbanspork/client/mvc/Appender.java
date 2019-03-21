package com.urbanspork.client.mvc;

import com.jfoenix.controls.JFXTextArea;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class Appender extends AppenderBase<ILoggingEvent> {

    private PatternLayout patternLayout;

    @Override
    public void start() {
        patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern("%d{HH:mm:ss} %msg%n");
        patternLayout.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        String msg = patternLayout.doLayout(eventObject);
        JFXTextArea logTextArea = Controller.INSTANCE.getLogTextArea();
        logTextArea.appendText(msg);
    }

}
