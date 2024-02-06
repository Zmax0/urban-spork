package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

record Session(Mode mode, Identity identity, Socks5CommandRequest request, ServerUserManager userManager, Context context) {}
