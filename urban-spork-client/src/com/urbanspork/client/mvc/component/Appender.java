package com.urbanspork.client.mvc.component;

import com.urbanspork.client.mvc.Components;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.scene.control.TextArea;

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
        TextArea logTextArea = Components.Controller.getLogTextArea();
        String log = logTextArea.textProperty().get();
        if (log.length() > 10000) {
            logTextArea.appendText("Clear for log length is " + log.length());
            log = new String();
        }
        logTextArea.appendText(msg);
    }

}
