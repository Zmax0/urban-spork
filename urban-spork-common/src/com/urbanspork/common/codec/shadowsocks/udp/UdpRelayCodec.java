package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.protocol.shadowsocks.replay.PacketWindowFilter;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.LruCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ChannelHandler.Sharable
public class UdpRelayCodec extends MessageToMessageCodec<DatagramPacket, DatagramPacketWrapper> {
    private static final Logger logger = LoggerFactory.getLogger(UdpRelayCodec.class);
    private static final AttributeKey<Control> CONTROL = AttributeKey.valueOf(UdpRelayCodec.class, Control.class.getSimpleName());
    private final Mode mode;
    private final ServerUserManager userManager;
    private final AeadCipherCodec cipher;
    private final LruCache<SocketAddress, Control> netMap;
    private final LruCache<Long, Filter> filterMap = new LruCache<>(10240, Duration.ofMinutes(5), (k, v) -> logger.trace("filter map {} expire", k));

    public UdpRelayCodec(ServerConfig config, Mode mode, ServerUserManager userManager) {
        this.mode = mode;
        this.userManager = userManager;
        this.cipher = AeadCipherCodecs.get(config);
        if (mode == Mode.Client) {
            netMap = new LruCache<>(1024, Duration.ofMinutes(5), (k, v) -> logger.trace("net map {} expire", k));
        } else {
            netMap = null;
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) throws Exception {
        InetSocketAddress proxy = msg.proxy();
        if (proxy == null) {
            throw new EncoderException("Relay address is null");
        }
        ByteBuf in = Unpooled.buffer();
        DatagramPacket data = msg.packet();
        Control control;
        if (mode == Mode.Server) {
            control = ctx.channel().attr(CONTROL).get();
            Filter filter = filterMap.get(control.getClientSessionId());
            control.setPacketId(filter.increasePacketId(1));
        } else {
            control = netMap.computeIfAbsent(data.sender(), k -> new Control());
            control.increasePacketId(1);
        }
        logger.trace("[udp][{}][encode]{}|{}", mode, proxy, control);
        cipher.encode(new Context(mode, control, data.recipient(), userManager), data.content(), in);
        out.add(new DatagramPacket(in, proxy, data.sender()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        Control control = new Control(0, 0, 0);
        Context context = new Context(mode, control, null, userManager);
        RelayingPacket<ByteBuf> packet = cipher.decode(context, msg.content());
        logger.trace("[udp][{}][decode]{}|{}", mode, msg.sender(), control);
        Channel channel = ctx.channel();
        long clientSessionId = control.getClientSessionId();
        Filter filter;
        long packetId;
        if (mode == Mode.Server) {
            channel.attr(AttributeKeys.SERVER_UDP_RELAY_WORKER).set(clientSessionId);
            filter = filterMap.computeIfAbsent(clientSessionId, k -> {
                control.setServerSessionId(ThreadLocalRandom.current().nextLong());
                channel.attr(CONTROL).set(control);
                return new Filter(new PacketWindowFilter(), 0);
            });
            packetId = control.getPacketId();
        } else {
            filter = filterMap.computeIfAbsent(clientSessionId, k -> new Filter(new PacketWindowFilter(), control.getPacketId()));
            filter.packetId = control.getPacketId();
            packetId = filter.packetId;
        }
        if (cipher instanceof Aead2022CipherCodecImpl && !filter.filter.validatePacketId(packetId, Long.MAX_VALUE)) {
            logger.error("packet id out of window, {}→{}|{}", msg.sender(), packet.address(), control);
            return;
        }
        out.add(new DatagramPacket(packet.content(), packet.address(), msg.sender()));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        filterMap.release();
        if (netMap != null) {
            netMap.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("udp replay error", cause);
    }

    private static class Filter {
        PacketWindowFilter filter;
        long packetId;

        Filter(PacketWindowFilter filter, long packetId) {
            this.filter = filter;
            this.packetId = packetId;
        }

        public long increasePacketId(long i) {
            packetId = Math.addExact(packetId, i);
            return packetId;
        }
    }
}