package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.LruCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

public class UdpRelayCodec extends MessageToMessageCodec<DatagramPacket, DatagramPacketWrapper> {
    private static final Logger logger = LoggerFactory.getLogger(UdpRelayCodec.class);
    private final ServerConfig config;
    private final Mode mode;
    private final ServerUserManager userManager;
    private final AeadCipherCodec cipher;
    private final LruCache<InetSocketAddress, Control> controlMap;
    private final Control control0;

    public UdpRelayCodec(ServerConfig config, Mode mode, ServerUserManager userManager) {
        this.config = config;
        this.mode = mode;
        this.userManager = userManager;
        this.cipher = AeadCipherCodecs.get(config);
        this.controlMap = new LruCache<>(1024, Duration.ofMinutes(5), (k, v) -> logger.info("[udp]control map expire {}={}", k, v));
        this.control0 = new Control(config.getCipher());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) throws Exception {
        InetSocketAddress proxy = msg.proxy();
        if (proxy == null) {
            throw new EncoderException("Relay address is null");
        }
        ByteBuf in = Unpooled.buffer();
        DatagramPacket data = msg.packet();
        Control control = getControl(proxy);
        control.increasePacketId(1);
        logger.trace("[udp][{}][encode]{}|{}", mode, proxy, control);
        cipher.encode(new Context(mode, control, data.recipient(), userManager), data.content(), in);
        out.add(new DatagramPacket(in, proxy, data.sender()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        Control control = getControl(msg.sender());
        Context context = new Context(mode, control, null, userManager);
        RelayingPacket<ByteBuf> packet = cipher.decode(context, msg.content());
        logger.trace("[udp][{}][decode]{}|{}", mode, msg.sender(), control);
        if (cipher instanceof Aead2022CipherCodecImpl && !control.validatePacketId()) {
            logger.error("[udp][{}→]{} packet_id {} out of window", mode, msg.sender(), control.getPacketId());
            return;
        }
        out.add(new DatagramPacket(packet.content(), packet.address(), msg.sender()));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        controlMap.release();
    }

    private Control getControl(InetSocketAddress key) {
        if (Mode.Client == mode) {
            return control0;
        } else {
            return controlMap.computeIfAbsent(key, k -> new Control(config.getCipher()));
        }
    }
}