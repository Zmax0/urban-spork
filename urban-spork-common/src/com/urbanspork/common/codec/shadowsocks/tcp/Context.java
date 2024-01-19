package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Session;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

record Context(Mode mode, Session session, Socks5CommandRequest request, ServerUserManager userManager) {}
