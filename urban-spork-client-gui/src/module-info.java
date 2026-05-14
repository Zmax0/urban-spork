module com.urbanspork.client.gui {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.jfoenix;
    requires com.urbanspork.client;
    requires com.urbanspork.common;
    requires io.netty.common;
    requires io.netty.handler;
    requires io.netty.transport;
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;
    requires tools.jackson.databind;

    exports com.urbanspork.client.gui.console to javafx.graphics, ch.qos.logback.core;
    exports com.urbanspork.client.gui.tray to javafx.graphics, ch.qos.logback.core;
}