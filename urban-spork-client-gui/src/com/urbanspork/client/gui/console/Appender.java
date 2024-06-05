package com.urbanspork.client.gui.console;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.scene.control.TextArea;

public class Appender extends AppenderBase<ILoggingEvent> {

    private TextArea textArea;
    private PatternLayout patternLayout;

    @Override
    public void start() {
        textArea = new TextArea();
        textArea.setEditable(false);
        patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern("%d{HH:mm:ss} %msg%n");
        patternLayout.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        String msg = patternLayout.doLayout(eventObject);
        String log = textArea.textProperty().get();
        if (log.length() > 10000) {
            textArea.appendText("Clear for log length is " + log.length());
            textArea.textProperty().set("");
        }
        textArea.appendText(msg);
    }

    public TextArea getTextArea() {
        return textArea;
    }
}
