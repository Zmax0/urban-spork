package com.urbanspork.common.protocol.shadowsocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksAddressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final Socks5CommandRequest request;

    private volatile boolean encoded = false;

    public ShadowsocksAddressEncoder(Socks5CommandRequest request) {
        this.request = request;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (!encoded) {
            encoded = true;
            encodeAddress(request, out);
        }
        out.writeBytes(msg);
    }

    private void encodeAddress(Socks5CommandRequest msg, ByteBuf out) throws Exception {
        Socks5AddressType addressType = msg.dstAddrType();
        out.writeByte(addressType.byteValue());
        Socks5AddressEncoder.DEFAULT.encodeAddress(addressType, msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

}
