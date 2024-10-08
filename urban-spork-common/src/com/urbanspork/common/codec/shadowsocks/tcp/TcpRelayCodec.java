package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

public class TcpRelayCodec extends ByteToMessageCodec<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(TcpRelayCodec.class);
    private final Session session;
    private final AeadCipherCodec cipher;

    public TcpRelayCodec(Context context, ServerConfig config, Mode mode, ServerUserManager userManager) {
        this(context, config, null, mode, userManager);
    }

    public TcpRelayCodec(Context context, ServerConfig config, InetSocketAddress request, Mode mode, ServerUserManager userManager) {
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        switch (cause) {
            case TooShortHeaderException ignore -> exceptionCaught0(ctx, cause);
            case RepeatedNonceException ignore -> exceptionCaught0(ctx, cause);
            default -> ctx.fireExceptionCaught(cause);
        }
    }

    private void exceptionCaught0(ChannelHandlerContext ctx, Throwable cause) {
        String transLog = ExceptionHandler.transLog(ctx.channel());
        logger.error("[tcp][{}] {}", transLog, cause.getMessage());
        SocketChannel channel = (SocketChannel) ctx.channel();
        ctx.deregister();
        channel.config().setSoLinger(0);
        channel.close(); // send RST
    }
}