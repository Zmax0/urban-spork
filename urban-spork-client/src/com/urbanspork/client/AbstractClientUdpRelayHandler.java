package com.urbanspork.client;

import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.FutureListeners;
import com.urbanspork.common.util.LruCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public abstract class AbstractClientUdpRelayHandler<K, C extends Channel> extends SimpleChannelInboundHandler<DatagramPacketWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientUdpRelayHandler.class);
    protected final ClientChannelContext context;
    protected final LruCache<K, Promise<C>> binding;

    protected AbstractClientUdpRelayHandler(ClientChannelContext context, Duration keepAlive) {
        super(false);
        this.context = context;
        this.binding = new LruCache<>(1024, keepAlive, (k, f) -> {
            if (f.isSuccess()) {
                C channel = f.getNow();
                logger.info("[udp][binding][expire]{} != {}", k, channel.localAddress());
                channel.close();
            }
        });
    }

    protected abstract Object convertToWrite(DatagramPacketWrapper msg);

    protected abstract K getKey(DatagramPacketWrapper msg);

    protected abstract void newBindingChannel(Channel inboundChannel, K k, Promise<C> promise);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
        DatagramPacket packet = msg.packet();
        Channel inbound = ctx.channel();
        getBindingChannel(inbound, getKey(msg)).addListener(
            FutureListeners.success(outbound -> {
                logger.debug("[udp][{}]{}→{}~{}→{}", context.config().getProtocol(), packet.sender(), msg.server(), inbound.localAddress(), outbound.localAddress());
                outbound.writeAndFlush(convertToWrite(msg));
            }));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        logger.info("Clean binding");
        binding.release();
    }

    private Future<C> getBindingChannel(Channel inboundChannel, K key) {
        return binding.computeIfAbsent(key, _ -> {
            // complete on the inbound event loop so that the write listener fires there
            Promise<C> promise = inboundChannel.eventLoop().newPromise();
            promise.addListener((Future<C> f) -> {
                if (f.isSuccess()) {
                    C channel = f.getNow();
                    logger.info("[udp][binding]{} == {}", key, channel.localAddress());
                    channel.closeFuture().addListener(_ -> {
                        Future<C> removed = binding.remove(key);
                        if (removed != null) {
                            logger.info("[udp][binding]{} != {}", key, channel.localAddress());
                        }
                    });
                } else {
                    // do not cache a failed binding, otherwise the next packet would reuse it
                    binding.remove(key);
                    logger.warn("[udp][binding] connect {} failed", key, f.cause());
                }
            });
            newBindingChannel(inboundChannel, key, promise);
            return promise;
        });
    }
}
