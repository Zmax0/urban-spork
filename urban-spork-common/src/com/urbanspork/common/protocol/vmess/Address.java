package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.vmess.header.AddressType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.NetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public interface Address {

    static void writeAddressPort(ByteBuf buf, Socks5CommandRequest address) {
        buf.writeShort(address.dstPort());
        Socks5AddressType addressType = address.dstAddrType();
        String addressString = address.dstAddr();
        if (Socks5AddressType.IPv4.equals(addressType)) {
            if (addressString != null) {
                buf.writeByte(AddressType.IPV4.getValue());
                buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(addressString));
            } else {
                buf.writeInt(0);
            }
        } else if (Socks5AddressType.IPv6.equals(addressType)) {
            if (addressString != null) {
                buf.writeByte(AddressType.IPV6.getValue());
                buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(addressString));
            } else {
                buf.writeLong(0);
                buf.writeLong(0);
            }
        } else if (Socks5AddressType.DOMAIN.equals(addressType)) { // port[2] + type[1] + domain_len[1] + domain_bytes[n]
            if (addressString != null) {
                byte[] domain = addressString.getBytes();
                buf.writeByte(AddressType.DOMAIN.getValue());
                buf.writeByte(domain.length);
                buf.writeBytes(domain);
            } else {
                buf.writeByte(0);
            }
        } else {
            throw new EncoderException("Unsupported addressType: " + (addressType.byteValue() & 0xFF));
        }
    }

    static InetSocketAddress readAddressPort(ByteBuf buf) {
        int port = buf.readUnsignedShort();
        String hostname;
        switch (AddressType.valueOf(buf.readByte())) {
            case IPV4 -> {
                byte[] bytes = new byte[4];
                buf.readBytes(bytes);
                hostname = NetUtil.bytesToIpAddress(bytes);
            }
            case IPV6 -> {
                byte[] bytes = new byte[16];
                buf.readBytes(bytes);
                hostname = NetUtil.bytesToIpAddress(bytes);
            }
            case DOMAIN -> {
                int length = buf.readByte();
                hostname = buf.readCharSequence(length, Charset.defaultCharset()).toString();
            }
            default -> throw new UnknownError();
        }
        return new InetSocketAddress(hostname, port);
    }
}
