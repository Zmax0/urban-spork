package com.urbanspork.client.gui.console.widget;

import ch.qos.logback.classic.Logger;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.Appender;
import com.urbanspork.client.gui.console.Console;
import javafx.scene.control.TextArea;
import org.slf4j.LoggerFactory;

public class ConsoleLogTextArea extends TextArea {
    public ConsoleLogTextArea(Console console) {
        setEditable(false);
        String name = Resource.application().getString("console.log.appender.name");
        if (LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) instanceof Logger root
            && root.getAppender(name) instanceof Appender appender) {
            appender.setConsole(console);
        }
    }
}
