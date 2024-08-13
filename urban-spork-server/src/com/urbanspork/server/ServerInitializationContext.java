package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;

public record ServerInitializationContext(ServerConfig config, Context context, ServerUserManager userManager) {
    public ServerInitializationContext(ServerConfig config, Context context) {
        this(config, context, ServerUserManager.from(config));
    }
}
