package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.vmess.header.AddressType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.NetUtil;

import java.nio.charset.Charset;

public interface Address {

    static void writeAddressPort(ByteBuf buf, Socks5CommandRequest address) {
        String dstAddr = address.dstAddr();
        if (dstAddr.isEmpty()) {
            throw new EncoderException("Empty destination address");
        }
        buf.writeShort(address.dstPort());
        Socks5AddressType addressType = address.dstAddrType();
        if (Socks5AddressType.IPv4.equals(addressType)) {
            buf.writeByte(AddressType.IPV4.getValue());
            buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(dstAddr));
        } else if (Socks5AddressType.IPv6.equals(addressType)) {
            buf.writeByte(AddressType.IPV6.getValue());
            buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(dstAddr));
        } else if (Socks5AddressType.DOMAIN.equals(addressType)) { // port[2] + type[1] + domain_len[1] + domain_bytes[n]
            byte[] domain = dstAddr.getBytes();
            buf.writeByte(AddressType.DOMAIN.getValue());
            buf.writeByte(domain.length);
            buf.writeBytes(domain);
        } else {
            throw new EncoderException("Unsupported addressType: " + (addressType.byteValue() & 0xFF));
        }
    }

    static Socks5CommandRequest readAddressPort(ByteBuf buf) {
        int port = buf.readUnsignedShort();
        String hostname = null;
        Socks5AddressType type = null;
        switch (AddressType.valueOf(buf.readByte())) {
            case IPV4 -> {
                type = Socks5AddressType.IPv4;
                byte[] bytes = new byte[4];
                buf.readBytes(bytes);
                hostname = NetUtil.bytesToIpAddress(bytes);
            }
            case IPV6 -> {
                type = Socks5AddressType.IPv6;
                byte[] bytes = new byte[16];
                buf.readBytes(bytes);
                hostname = NetUtil.bytesToIpAddress(bytes);
            }
            case DOMAIN -> {
                type = Socks5AddressType.DOMAIN;
                int length = buf.readByte();
                hostname = buf.readCharSequence(length, Charset.defaultCharset()).toString();
            }
        }
        return new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, type, hostname, port);
    }
}
