package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.util.List;

public class TcpRelayCodec extends ByteToMessageCodec<ByteBuf> {
    private final Session session;
    private final AeadCipherCodec cipher;

    public TcpRelayCodec(Context context, ServerConfig config, Mode mode) {
        this(context, config, null, mode);
    }

    public TcpRelayCodec(Context context, ServerConfig config, Socks5CommandRequest request, Mode mode) {
        ServerUserManager userManager = Mode.Server == mode ? ServerUserManager.DEFAULT : ServerUserManager.EMPTY;
        Identity identity = new Identity(config.getCipher());
        this.session = new Session(mode, identity, request, userManager, context);
        this.cipher = AeadCipherCodecs.get(config);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        cipher.encode(session, msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        cipher.decode(session, in, out);
    }
}