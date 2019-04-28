package com.urbanspork.client.mvc.component;

import com.jfoenix.controls.JFXTextArea;
import com.urbanspork.client.mvc.Component;

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
        Controller controller = Component.Controller.get();
        JFXTextArea logTextArea = controller.getLogTextArea();
        String log = logTextArea.textProperty().get();
        if (log.length() > 10000) {
            logTextArea.appendText("Clear for log length is " + log.length());
            log = new String();
        }
        logTextArea.appendText(msg);
    }

}
