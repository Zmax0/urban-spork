package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Control;

import java.net.InetSocketAddress;

record Context(Mode mode, Control control, InetSocketAddress address, ServerUserManager userManager) {}
