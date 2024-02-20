package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.vmess.header.AddressType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public interface Address {

    static void writeAddressPort(ByteBuf buf, InetSocketAddress address) {
        if (address == null) {
            throw new EncoderException("Empty destination address");
        }
        buf.writeShort(address.getPort());
        InetAddress ip = address.getAddress();
        if (ip instanceof Inet4Address ipv4) {
            buf.writeByte(AddressType.IPV4.getValue());
            buf.writeBytes(ipv4.getAddress());
        } else if (ip instanceof Inet6Address ipv6) {
            buf.writeByte(AddressType.IPV6.getValue());
            buf.writeBytes(ipv6.getAddress());
        } else { // port[2] + type[1] + domain_len[1] + domain_bytes[n]
            byte[] domain = address.getHostString().getBytes();
            buf.writeByte(AddressType.DOMAIN.getValue());
            buf.writeByte(domain.length);
            buf.writeBytes(domain);
        }
    }

    static InetSocketAddress readAddressPort(ByteBuf buf) {
        int port = buf.readUnsignedShort();
        AddressType addressType = AddressType.valueOf(buf.readByte());
        if (AddressType.IPV4 == addressType) {
            byte[] bytes = new byte[4];
            buf.readBytes(bytes);
            try {
                return new InetSocketAddress(InetAddress.getByAddress(bytes), port);
            } catch (UnknownHostException ignore) {
                // should never be caught
            }
        } else if (AddressType.IPV6 == addressType) {
            byte[] bytes = new byte[16];
            buf.readBytes(bytes);
            try {
                return new InetSocketAddress(InetAddress.getByAddress(bytes), port);
            } catch (UnknownHostException ignore) {
                // should never be caught
            }
        }
        int length = buf.readByte();
        String hostname = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
        return InetSocketAddress.createUnresolved(hostname, port);
    }
}
