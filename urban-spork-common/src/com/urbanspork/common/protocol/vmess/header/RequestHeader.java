package com.urbanspork.common.protocol.vmess.header;

import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;

import java.net.InetSocketAddress;

public record RequestHeader(byte version, RequestCommand command, RequestOption[] option, SecurityType security, InetSocketAddress address, byte[] id) {
    public static RequestHeader defaultHeader(SecurityType security, RequestCommand command, InetSocketAddress address, String uuid) {
        return new RequestHeader(VMess.VERSION, command, new RequestOption[]{RequestOption.ChunkStream, RequestOption.ChunkMasking, RequestOption.GlobalPadding, RequestOption.AuthenticatedLength},
            security, address, ID.newID(uuid));
    }
}
