package com.urbanspork.protocol;

import java.net.InetSocketAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksProtocolCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(ShadowsocksProtocolCodec.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        msg.retain();
        Socks5CommandRequest request = ctx.channel().attr(Attributes.REQUEST).get();
        if (request != null) {
            logger.debug("Encode request: {}", request.dstAddr() + ':' + request.dstPort());
            ctx.channel().attr(Attributes.REQUEST).set(null);
            out.add(Unpooled.wrappedBuffer(ShadowsocksProtocol.encodeRequest(request), msg));
        } else {
            out.add(msg);
        }
    }

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
