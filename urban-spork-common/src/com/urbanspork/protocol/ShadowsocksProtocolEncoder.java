package com.urbanspork.protocol;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksProtocolEncoder extends MessageToMessageEncoder<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(ShadowsocksProtocolEncoder.class);

    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Socks5CommandRequest request = ctx.channel().attr(Attributes.REQUEST).get();
        if (request != null) {
            logger.debug("Encode request: {}", request.dstAddr() + ':' + request.dstPort());
            ctx.channel().attr(Attributes.REQUEST).set(null);
            ByteBuf buff = ShadowsocksProtocol.encodeRequest(request);
            if (msg.isReadable()) {
                buff.writeBytes(msg);
            }
            out.add(buff);
        } else {
            out.add(msg.retain());
        }
    }

}
