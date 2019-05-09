package com.urbanspork.protocol;

import java.net.InetSocketAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class ShadowsocksProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(ShadowsocksProtocolDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        msg.retain();
        InetSocketAddress address = ShadowsocksProtocol.decodeAddress(msg);
        if (address != null) {
            logger.debug("Decode address: {}", address.getHostName() + ':' + address.getPort());
            ctx.channel().attr(Attributes.REMOTE_ADDRESS).set(address);
        }
        out.add(msg);
    }

}
