package com.urbanspork.common.protocol.shadowsocks;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;

import java.util.List;

import static com.urbanspork.common.protocol.shadowsocks.ShadowsocksProtocolDecoder.State.INIT;
import static com.urbanspork.common.protocol.shadowsocks.ShadowsocksProtocolDecoder.State.SUCCESS;

public class ShadowsocksProtocolDecoder extends ReplayingDecoder<ShadowsocksProtocolDecoder.State> implements ShadowsocksProtocol {

    public ShadowsocksProtocolDecoder() {
        super(INIT);
    }

    enum State {
        INIT,
        SUCCESS,
        FAILURE
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case INIT -> {
                Socks5AddressType addressType = Socks5AddressType.valueOf(in.getByte(0));
                if (addressType == Socks5AddressType.DOMAIN) {
                    int length = in.getByte(1);
                    if (in.readableBytes() >= length + 4) {
                        in.skipBytes(1);
                        out.add(decodeAddress(addressType, in));
                        checkpoint(SUCCESS);
                    }
                } else if (addressType == Socks5AddressType.IPv4 && in.readableBytes() >= 7) {
                    in.skipBytes(1);
                    out.add(decodeAddress(addressType, in));
                    checkpoint(SUCCESS);
                } else if (addressType == Socks5AddressType.IPv6 && in.readableBytes() >= 19) {
                    in.skipBytes(1);
                    out.add(decodeAddress(addressType, in));
                    checkpoint(SUCCESS);
                }
            }
            case SUCCESS -> {
                int readableBytes = actualReadableBytes();
                if (readableBytes > 0) {
                    out.add(in.readRetainedSlice(readableBytes));
                }
            }
            default -> in.skipBytes(actualReadableBytes());
        }
    }
}
