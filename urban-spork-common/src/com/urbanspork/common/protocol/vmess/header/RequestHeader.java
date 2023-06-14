package com.urbanspork.common.protocol.vmess.header;

import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public record RequestHeader(byte version, RequestCommand command, RequestOption[] option, SecurityType security, Socks5CommandRequest address, byte[] id) {
    public static RequestHeader defaultHeader(SecurityType security, Socks5CommandRequest address, String uuid) {
        return new RequestHeader(VMess.VERSION, RequestCommand.TCP, new RequestOption[]{RequestOption.ChunkStream, RequestOption.AuthenticatedLength},
            security, address, ID.newID(uuid));
    }
}
