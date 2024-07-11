module com.urbanspork.client.gui {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.fasterxml.jackson.databind;
    requires com.jfoenix;
    requires com.urbanspork.client;
    requires com.urbanspork.common;
    requires io.netty.handler;
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;

    exports com.urbanspork.client.gui.console to javafx.graphics, ch.qos.logback.core;
    exports com.urbanspork.client.gui.tray to javafx.graphics, ch.qos.logback.core;
}