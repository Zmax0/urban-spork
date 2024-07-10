package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Identity;

import java.net.InetSocketAddress;

record Session(Mode mode, Identity identity, InetSocketAddress request, ServerUserManager userManager, Context context) {}
