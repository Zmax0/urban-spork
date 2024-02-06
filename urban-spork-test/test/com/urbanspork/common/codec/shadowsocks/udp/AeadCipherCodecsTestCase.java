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
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - AEAD Cipher UDP Codecs")
@TestInstance(Lifecycle.PER_CLASS)
class AeadCipherCodecsTestCase {

    private final byte[] in = Dice.rollBytes(0xffff * 10);
    private CipherKind kind;
    private String password;

    @BeforeAll
    void beforeAll() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(AeadCipherCodec.class);
        logger.setLevel(Level.TRACE);
    }

    @DisplayName("Single cipher")
    @Test
    void test() throws Exception {
        parameterizedTest(CipherKind.chacha20_poly1305);
        parameterizedTest(CipherKind.aead2022_blake3_aes_256_gcm);
    }

    @ParameterizedTest
    @DisplayName("All supported cipher iterate")
    @EnumSource(CipherKind.class)
    void parameterizedTest(CipherKind kind) throws Exception {
        this.password = TestDice.rollPassword(Protocol.shadowsocks, kind);
        this.kind = kind;
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        cipherTest(new Context(Mode.Client, new Control(kind), address, ServerUserManager.EMPTY), new Context(Mode.Server, new Control(kind), null, ServerUserManager.EMPTY));
    }

    private void cipherTest(Context request, Context response) throws Exception {
        List<Object> list = cipherTest(request, response, Unpooled.copiedBuffer(in));
        byte[] out = new byte[in.length];
        int len = 0;
        for (Object obj : list) {
            if (obj instanceof ByteBuf outBuf) {
                int readableBytes = outBuf.readableBytes();
                outBuf.readBytes(out, len, readableBytes);
                outBuf.release();
                len += readableBytes;
            }
        }
        Assertions.assertEquals(in.length, len);
        Assertions.assertArrayEquals(in, out);
    }

    private List<Object> cipherTest(Context request, Context response, ByteBuf in)
        throws Exception {
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
        List<Object> out = new ArrayList<>();
        for (ByteBuf buf : encodeSlices) {
            server.decode(response, buf, out);
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