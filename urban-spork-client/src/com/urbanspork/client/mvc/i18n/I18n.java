package com.urbanspork.client.mvc.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import com.urbanspork.client.mvc.Resource;

public final class I18n {

    private static final ResourceBundle BUNDLE = Resource.bundle();

    public static final Locale[] LANGUAGES = new Locale[] { Locale.CHINESE, Locale.ENGLISH };

    public static final String PROGRAM_TITLE = BUNDLE.getString("program.title");
    public static final String TRAY_TOOLTIP = BUNDLE.getString("tray.tooltip");
    public static final String TRAY_EXIT = BUNDLE.getString("tray.exit");
    public static final String TRAY_MENU_CONSOLE = BUNDLE.getString("tray.menu.console");
    public static final String TRAY_MENU_LANGUAGE = BUNDLE.getString("tray.menu.language");
    public static final String TRAY_MENU_SERVERS = BUNDLE.getString("tray.menu.servers");
    public static final String CONSOLE_TAB0_TEXT = BUNDLE.getString("console.tab0.text");
    public static final String CONSOLE_TAB1_TEXT = BUNDLE.getString("console.tab1.text");
    public static final String CONSOLE_BUTTON_ADD = BUNDLE.getString("console.button.add");
    public static final String CONSOLE_BUTTON_DEL = BUNDLE.getString("console.button.del");
    public static final String CONSOLE_BUTTON_COPY = BUNDLE.getString("console.button.copy");
    public static final String CONSOLE_BUTTON_UP = BUNDLE.getString("console.button.up");
    public static final String CONSOLE_BUTTON_DOWN = BUNDLE.getString("console.button.down");
    public static final String CONSOLE_BUTTON_CONFIRM = BUNDLE.getString("console.button.confirm");
    public static final String CONSOLE_BUTTON_CANCEL = BUNDLE.getString("console.button.cancel");
    public static final String CONSOLE_LABEL_HOST = BUNDLE.getString("console.label.host");
    public static final String CONSOLE_LABEL_PORT = BUNDLE.getString("console.label.port");
    public static final String CONSOLE_LABEL_PASSWORD = BUNDLE.getString("console.label.password");
    public static final String CONSOLE_LABEL_CIPHER = BUNDLE.getString("console.label.cipher");
    public static final String CONSOLE_LABEL_REMARK = BUNDLE.getString("console.label.remark");
    public static final String CONSOLE_LABEL_PROXY_PORT = BUNDLE.getString("console.label.proxy.port");
    public static final String CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE = BUNDLE.getString("console.validator.required.field.message");

}
