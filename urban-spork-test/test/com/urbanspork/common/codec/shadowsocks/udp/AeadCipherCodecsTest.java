package com.urbanspork.common.codec.shadowsocks.udp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("udp.AeadCipherCodecTest")
class AeadCipherCodecsTest {

    private final byte[] in = Dice.rollBytes(0xffff * 10);
    private CipherKind kind;
    private String password;

    @BeforeAll
    void beforeAll() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(AeadCipherCodec.class);
        logger.setLevel(Level.TRACE);
    }

    @ParameterizedTest
    @EnumSource(CipherKind.class)
    void testByKind(CipherKind kind) throws Exception {
        this.password = TestDice.rollPassword(Protocol.shadowsocks, kind);
        this.kind = kind;
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        cipherTest(new Context(Mode.Client, new Control(), address, ServerUserManager.empty()), new Context(Mode.Server, new Control(), null, ServerUserManager.empty()));
    }

    private void cipherTest(Context request, Context response) throws Exception {
        List<RelayingPacket<ByteBuf>> list = cipherTest(request, response, Unpooled.copiedBuffer(in));
        byte[] out = new byte[in.length];
        int len = 0;
        for (RelayingPacket<ByteBuf> packet : list) {
            ByteBuf msg = packet.content();
            int readableBytes = msg.readableBytes();
            msg.readBytes(out, len, readableBytes);
            msg.release();
            len += readableBytes;
        }
        Assertions.assertEquals(in.length, len);
        Assertions.assertArrayEquals(in, out);
    }

    private List<RelayingPacket<ByteBuf>> cipherTest(Context request, Context response, ByteBuf in)
        throws InvalidCipherTextException {
        ServerConfig config = new ServerConfig();
        config.setCipher(kind);
        config.setPassword(password);
        AeadCipherCodec client = AeadCipherCodecs.get(config);
        AeadCipherCodec server = AeadCipherCodecs.get(config);
        List<ByteBuf> encodeSlices = new ArrayList<>();
        for (ByteBuf slice : randomSlice(in)) {
            ByteBuf buf = Unpooled.buffer();
            client.encode(request, slice, buf);
            encodeSlices.add(buf);
        }
        List<RelayingPacket<ByteBuf>> out = new ArrayList<>();
        for (ByteBuf buf : encodeSlices) {
            out.add(server.decode(response, buf));
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        while (src.isReadable()) {
            int maxChunkSize = kind.isAead2022() ? 0xffff - 100 : 0x3fff - 100;
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(1, maxChunkSize))));
        }
        return list;
    }

}