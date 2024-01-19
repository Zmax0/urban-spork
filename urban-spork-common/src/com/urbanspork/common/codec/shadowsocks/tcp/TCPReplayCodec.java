package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Session;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.util.List;

public class TCPReplayCodec extends ByteToMessageCodec<ByteBuf> {

    private final Context context;
    private final AeadCipherCodec cipher;

    public TCPReplayCodec(Mode mode, ServerConfig config) {
        this(mode, null, config);
    }

    public TCPReplayCodec(Mode mode, Socks5CommandRequest request, ServerConfig config) {
        ServerUserManager userManager = Mode.Server == mode ? ServerUserManager.DEFAULT : ServerUserManager.EMPTY;
        this.context = new Context(mode, new Session(config.getCipher()), request, userManager);
        this.cipher = AeadCipherCodecs.get(config);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        cipher.encode(context, msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        cipher.decode(context, in, out);
    }
}
