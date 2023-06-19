package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    private final ServerConfig config;

    public ExceptionHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        if (cause.getCause() instanceof InvalidCipherTextException) {
            logger.error("[{}][{} → {}] Invalid cipher text", config.getProtocol(), channel.localAddress(), channel.remoteAddress());
        } else {
            String msg = String.format("[%s][%s → %s] Caught exception", config.getProtocol(), channel.localAddress(), channel.remoteAddress());
            logger.error(msg, cause);
        }
        ctx.close();
    }
}
