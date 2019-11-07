package com.urbanspork.common.protocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.CharsetUtil;

public interface ShadowsocksProtocol {

    default ByteBuf encodeRequest(Socks5CommandRequest request) {
        ByteBuf buf = Unpooled.directBuffer();
        String host = request.dstAddr();
        int port = request.dstPort();
        buf.writeByte(request.dstAddrType().byteValue());
        byte[] hostBytes = host.getBytes(CharsetUtil.US_ASCII);
        buf.writeByte(hostBytes.length);
        buf.writeBytes(hostBytes);
        buf.writeShort(port);
        return buf;
    }

    default InetSocketAddress decodeAddress(ByteBuf msg) throws Exception {
        Socks5AddressType addressType = Socks5AddressType.valueOf(msg.getByte(0));
        if (addressType == Socks5AddressType.DOMAIN) {
            int length = (int) msg.getByte(1);
            if (msg.readableBytes() >= length + 4) {
                msg.readerIndex(msg.readerIndex() + 2);
                String host = msg.readCharSequence(length, CharsetUtil.US_ASCII).toString();
                int port = msg.readUnsignedShort();
                return new InetSocketAddress(host, port);
            }
        } else if (addressType == Socks5AddressType.IPv4 && msg.readableBytes() >= 7) {
            msg.readerIndex(msg.readerIndex() + 1);
            byte[] ip = new byte[4];
            msg.readBytes(ip);
            String host = InetAddress.getByAddress(ip).toString().substring(1);
            int port = msg.readShort();
            return new InetSocketAddress(host, port);
        }
        throw new IllegalArgumentException("Unknown address type: " + addressType);
    }

}
