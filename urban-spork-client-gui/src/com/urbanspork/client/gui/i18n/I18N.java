package com.urbanspork.client.gui.i18n;

import com.urbanspork.client.gui.Resource;

import java.util.Locale;

public interface I18N {
    String PROGRAM_TITLE = "program.title";
    String TRAY_TOOLTIP = "tray.tooltip";
    String TRAY_EXIT = "tray.exit";
    String TRAY_MENU_CONSOLE = "tray.menu.console";
    String TRAY_MENU_LANGUAGE = "tray.menu.language";
    String TRAY_MENU_SERVERS = "tray.menu.servers";
    String CONSOLE_TAB0_TEXT = "console.tab0.text";
    String CONSOLE_TAB1_TEXT = "console.tab1.text";
    String CONSOLE_TAB2_TEXT = "console.tab2.text";
    String CONSOLE_BUTTON_NEW = "console.button.new";
    String CONSOLE_BUTTON_IMPORT = "console.button.import";
    String CONSOLE_BUTTON_SHARE = "console.button.share";
    String CONSOLE_BUTTON_DEL = "console.button.del";
    String CONSOLE_BUTTON_COPY = "console.button.copy";
    String CONSOLE_BUTTON_UP = "console.button.up";
    String CONSOLE_BUTTON_DOWN = "console.button.down";
    String CONSOLE_BUTTON_CONFIRM = "console.button.confirm";
    String CONSOLE_BUTTON_CANCEL = "console.button.cancel";
    String CONSOLE_LABEL_HOST = "console.label.host";
    String CONSOLE_LABEL_PORT = "console.label.port";
    String CONSOLE_LABEL_PASSWORD = "console.label.password";
    String CONSOLE_LABEL_CIPHER = "console.label.cipher";
    String CONSOLE_LABEL_PROTOCOL = "console.label.protocol";
    String CONSOLE_LABEL_REMARK = "console.label.remark";
    String CONSOLE_LABEL_PROXY_PORT = "console.label.proxy.port";
    String CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE = "console.validator.required.field.message";

    static Locale[] languages() {
        return new Locale[]{Locale.CHINESE, Locale.ENGLISH};
    }

    static String getString(String key) {
        return Resource.language().getString(key);
    }
}
