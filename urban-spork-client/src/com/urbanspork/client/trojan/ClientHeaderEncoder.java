package com.urbanspork.client.trojan;

import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ClientHeaderEncoder extends MessageToByteEncoder<ByteBuf> {

    private final byte[] key;
    private final InetSocketAddress address;
    private final byte command;

    public ClientHeaderEncoder(String password, InetSocketAddress address, byte command) {
        this.key = ByteBufUtil.hexDump(Digests.sha224.hash(password.getBytes(StandardCharsets.US_ASCII))).getBytes();
        this.address = address;
        this.command = command;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        out.writeBytes(key);
        out.writeBytes(Trojan.CRLF);
        out.writeByte(command);
        Address.encode(address, out);
        out.writeBytes(Trojan.CRLF);
        out.writeBytes(msg);
        ctx.pipeline().remove(this);
    }
}
