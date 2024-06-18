package com.urbanspork.server.trojan;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.server.ServerRelayHandler;
import com.urbanspork.server.ServerUdpRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socks.SocksCmdType;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ServerHeaderDecoder extends ByteToMessageDecoder {
    private final ServerConfig config;
    private final byte[] password;

    public ServerHeaderDecoder(ServerConfig config) {
        this.config = config;
        this.password = Digests.sha224.hash(config.getPassword().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 60 || in.readableBytes() < 59 + Address.requireLength(in, 59)) {
            return;
        }
        if (in.getByte(56) != '\r') {
            throw new DecoderException("not trojan protocol");
        }
        if (!Arrays.equals(ByteBufUtil.decodeHexDump(in.readCharSequence(56, StandardCharsets.US_ASCII)), password)) {
            throw new DecoderException("not a valid password");
        }
        in.skipBytes(Trojan.CRLF.length);
        byte command = in.readByte(); // command
        InetSocketAddress address = Address.decode(in);
        in.skipBytes(Trojan.CRLF.length);
        ChannelPipeline p = ctx.pipeline();
        if (SocksCmdType.UDP.byteValue() == command) {
            p.addAfter(ctx.name(), null, new ServerUdpRelayHandler(config.getPacketEncoding(), ctx.channel().eventLoop().parent().next()));
            p.addAfter(ctx.name(), null, new ServerPacketCodec(address));
            p.remove(this);
        } else {
            p.addAfter(ctx.name(), null, new ServerRelayHandler(config));
            out.add(new RelayingPayload<>(address, Unpooled.EMPTY_BUFFER));
            p.remove(this);
        }
    }
}
