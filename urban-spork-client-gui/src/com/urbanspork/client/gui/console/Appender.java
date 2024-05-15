package com.urbanspork.client.gui.console;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.scene.control.TextArea;

public class Appender extends AppenderBase<ILoggingEvent> {

    private Console console;
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
        TextArea logTextArea = console.getLogTextArea();
        String log = logTextArea.textProperty().get();
        if (log.length() > 10000) {
            logTextArea.appendText("Clear for log length is " + log.length());
            logTextArea.textProperty().set("");
        }
        logTextArea.appendText(msg);
    }

    public void setConsole(Console console) {
        this.console = console;
    }
}
