package com.urbanspork.common.protocol.socks;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.CharsetUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public interface Address {

    static void encode(InetSocketAddress address, ByteBuf out) {
        if (address.isUnresolved()) {
            String hostName = address.getHostName();
            out.writeByte(Socks5AddressType.DOMAIN.byteValue());
            out.writeByte(hostName.length());
            out.writeCharSequence(hostName, CharsetUtil.US_ASCII);
        } else {
            InetAddress ipAddress = address.getAddress();
            if (ipAddress instanceof Inet4Address) {
                out.writeByte(Socks5AddressType.IPv4.byteValue());
            } else {
                out.writeByte(Socks5AddressType.IPv6.byteValue());
            }
            out.writeBytes(ipAddress.getAddress());
        }
        out.writeShort(address.getPort());
    }

    static void encode(Socks5CommandRequest request, ByteBuf out) {
        Socks5AddressType addressType = request.dstAddrType();
        out.writeByte(addressType.byteValue());
        try {
            Socks5AddressEncoder.DEFAULT.encodeAddress(addressType, request.dstAddr(), out);
        } catch (Exception e) {
            throw new EncoderException(e);
        }
        out.writeShort(request.dstPort());
    }

    static InetSocketAddress decode(ByteBuf in) {
        Socks5AddressType addressType = Socks5AddressType.valueOf(in.readByte());
        if (addressType == Socks5AddressType.IPv4) {
            byte[] bytes = new byte[4];
            in.readBytes(bytes);
            InetAddress addr = null;
            try {
                addr = InetAddress.getByAddress(bytes);
            } catch (UnknownHostException _) {
                // should never be caught
            }
            int port = in.readUnsignedShort();
            return new InetSocketAddress(addr, port);
        } else if (addressType == Socks5AddressType.IPv6) {
            byte[] bytes = new byte[16];
            in.readBytes(bytes);
            InetAddress addr = null;
            try {
                addr = InetAddress.getByAddress(bytes);
            } catch (UnknownHostException _) {
                // should never be caught
            }
            int port = in.readUnsignedShort();
            return new InetSocketAddress(addr, port);
        } else if (addressType == Socks5AddressType.DOMAIN) {
            int length = in.readUnsignedByte();
            String host = in.readCharSequence(length, CharsetUtil.US_ASCII).toString();
            int port = in.readUnsignedShort();
            return InetSocketAddress.createUnresolved(host, port);
        } else {
            throw new DecoderException("unsupported address type: " + (addressType.byteValue() & 0xFF));
        }
    }

    static int getLength(Socks5CommandRequest request) {
        Socks5AddressType addrType = request.dstAddrType();
        if (Socks5AddressType.IPv4.equals(addrType)) {
            return 1 + 4 + 2;
        } else if (Socks5AddressType.IPv6.equals(addrType)) {
            return 1 + 8 * 2 + 2;
        } else if (Socks5AddressType.DOMAIN.equals(addrType)) {
            return 1 + 1 + request.dstAddr().length() + 2;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static int getLength(InetSocketAddress address) {
        if (address.isUnresolved()) {
            return 1 + 1 + address.getHostName().length() + 2;
        } else {
            InetAddress ipAddress = address.getAddress();
            if (ipAddress instanceof Inet4Address) {
                return 1 + 4 + 2;
            } else {
                return 1 + 8 * 2 + 2;
            }
        }
    }

    static int requireLength(ByteBuf in, int index) {
        Socks5AddressType addressType = Socks5AddressType.valueOf(in.getByte(index));
        if (Socks5AddressType.IPv4.equals(addressType)) {
            return 1 + 4 + 2;
        } else if (Socks5AddressType.IPv6.equals(addressType)) {
            return 1 + 8 * 2 + 2;
        } else if (Socks5AddressType.DOMAIN.equals(addressType)) {
            return 1 + 1 + in.getUnsignedByte(index + 1) + 2;
        } else {
            throw new DecoderException("unsupported address type: " + (addressType.byteValue() & 0xFF));
        }
    }
}
