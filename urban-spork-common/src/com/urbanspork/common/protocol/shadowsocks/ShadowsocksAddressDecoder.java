package com.urbanspork.common.protocol.shadowsocks;


import com.urbanspork.common.protocol.socks.Socks5Addressing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class ShadowsocksAddressDecoder extends ReplayingDecoder<ShadowsocksAddressDecoder.State> {

    public ShadowsocksAddressDecoder() {
        super(State.INIT);
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
                Socks5Addressing.decode(in, out);
                checkpoint(State.SUCCESS);
            }
            case SUCCESS -> {
                int readableBytes = actualReadableBytes();
                if (readableBytes > 0) {
                    out.add(in.readRetainedSlice(readableBytes));
                }
            }
            case FAILURE -> in.skipBytes(actualReadableBytes());
        }
    }
}
