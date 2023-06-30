package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Direction;
import com.urbanspork.common.protocol.network.Network;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramChannel;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    private final ServerConfig config;

    public ExceptionHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        String network = channel instanceof DatagramChannel ? Network.UDP.value() : Network.TCP.value();
        Protocols protocol = config.getProtocol();
        Direction direction = channel.attr(AttributeKeys.DIRECTION).get();
        if (direction == null) {
            direction = Direction.Inbound;
        }
        String transLog = transLog(direction, channel.localAddress(), channel.remoteAddress());
        if (cause.getCause() instanceof InvalidCipherTextException) {
            logger.error("[{}][{}][{}] Invalid cipher text", network, protocol, transLog);
        } else {
            String msg = String.format("[%s][%s][%s] Caught exception", network, protocol, transLog);
            logger.error(msg, cause);
        }
        ctx.close();
    }

    private static String transLog(Direction direction, SocketAddress local, SocketAddress remote) {
        return local + direction.getValue() + remote;
    }
}
