package com.urbanspork.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public interface ShadowsocksProtocol {

    default byte[] encodeRequest(Socks5CommandRequest request) {
        String host = request.dstAddr();
        int port = request.dstPort();
        byte[] hostBytes = host.getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = new byte[hostBytes.length + 4];
        encoded[0] = request.dstAddrType().byteValue();
        encoded[1] = (byte) hostBytes.length;
        System.arraycopy(hostBytes, 0, encoded, 2, hostBytes.length);
        encoded[hostBytes.length + 2] = (byte) (port >>> 8);
        encoded[hostBytes.length + 3] = (byte) port;
        return encoded;
    }

    default InetSocketAddress decodeAddress(ByteBuf msg) throws UnknownHostException {
        Socks5AddressType addressType = Socks5AddressType.valueOf(msg.getByte(0));
        if (addressType == Socks5AddressType.DOMAIN) {
            int length = msg.getByte(1);
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
