package com.urbanspork.common.protocol.socks;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.*;

import java.net.InetSocketAddress;
import java.util.List;

public interface Address {

    static void encode(Socks5CommandType type, InetSocketAddress address, ByteBuf out) {
        encode(Socks5.toCommandRequest(type, address), out);
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

    static void decode(ByteBuf in, List<Object> out) {
        out.add(decode(in));
    }

    static InetSocketAddress decode(ByteBuf in) {
        Socks5AddressType addressType = Socks5AddressType.valueOf(in.readByte());
        String hostname;
        try {
            hostname = Socks5AddressDecoder.DEFAULT.decodeAddress(addressType, in);
        } catch (Exception e) {
            throw new DecoderException(e);
        }
        if (addressType == Socks5AddressType.DOMAIN) {
            return InetSocketAddress.createUnresolved(hostname, in.readUnsignedShort());
        } else {
            return new InetSocketAddress(hostname, in.readUnsignedShort());
        }
    }
}
