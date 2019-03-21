package com.urbanspork.protocol;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.CharsetUtil;

public interface ShadowsocksProtocol {

    static ByteBuf encodeRequest(Socks5CommandRequest request) {
        ByteBuf buf = Unpooled.buffer(128);
        String host = request.dstAddr();
        int port = request.dstPort();
        buf.writeByte(request.dstAddrType().byteValue());
        buf.writeByte(host.length());
        buf.writeBytes(host.getBytes(CharsetUtil.US_ASCII));
        buf.writeShort(port);
        return buf;
    }

    static InetSocketAddress decodeAddress(ByteBuf msg) throws Exception {
        Socks5AddressType addressType = Socks5AddressType.valueOf(msg.getByte(0));
        if (addressType == Socks5AddressType.DOMAIN) {
            msg.readerIndex(msg.readerIndex() + 1);
            int length = (int) msg.readByte();
            String host = msg.readCharSequence(length, CharsetUtil.US_ASCII).toString();
            int port = msg.readUnsignedShort();
            InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            return remoteAddress;
        } else {
            return null;
        }
    }

}
