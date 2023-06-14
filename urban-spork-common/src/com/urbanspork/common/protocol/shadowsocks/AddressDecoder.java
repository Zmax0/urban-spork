package com.urbanspork.common.protocol.shadowsocks;


import com.urbanspork.common.protocol.socks.Address;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class AddressDecoder extends ReplayingDecoder<AddressDecoder.State> {

    public AddressDecoder() {
        super(State.INIT);
    }

    enum State {
        INIT,
        SUCCESS
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        State state = state();
        if (state == State.INIT) {
            Address.decode(in, out);
            checkpoint(State.SUCCESS);
        } else {
            out.add(in.readRetainedSlice(actualReadableBytes()));
        }
    }
}
