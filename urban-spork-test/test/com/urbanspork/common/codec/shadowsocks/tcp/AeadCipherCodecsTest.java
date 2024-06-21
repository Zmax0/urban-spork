package com.urbanspork.common.codec.shadowsocks.tcp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@TestInstance(Lifecycle.PER_CLASS)
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
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, host, port);
        cipherTest(newContext(Mode.Client, kind, request), newContext(Mode.Server, kind, null));
    }

    private void cipherTest(Session request, Session response) throws Exception {
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
            if (obj instanceof RelayingPayload<?> payload) {
                ByteBuf outBuf = (ByteBuf) payload.content();
                int readableBytes = outBuf.readableBytes();
                outBuf.readBytes(out, len, readableBytes);
                outBuf.release();
                len += readableBytes;
            }
        }
        Assertions.assertEquals(in.length, len);
        Assertions.assertArrayEquals(in, out);
    }

    private List<Object> cipherTest(Session request, Session response, ByteBuf in) throws Exception {
        ServerConfig config = new ServerConfig();
        config.setCipher(kind);
        config.setPassword(password);
        AeadCipherCodec client = AeadCipherCodecs.get(config);
        AeadCipherCodec server = AeadCipherCodecs.get(config);
        List<ByteBuf> encodeSlices = new ArrayList<>();
        for (ByteBuf slice : randomSlice(in, false)) {
            ByteBuf buf = Unpooled.buffer();
            client.encode(request, slice, buf);
            encodeSlices.add(buf);
        }
        List<Object> out = new ArrayList<>();
        ByteBuf buffer = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(merge(encodeSlices), true)) {
            buffer.writeBytes(slice);
            server.decode(response, buffer, out);
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src, boolean firstSmallSlice) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        if (firstSmallSlice) {
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(1024, 10240))));
        }
        while (src.isReadable()) {
            int maxChunkSize = kind.isAead2022() ? 0xffff - 100 : 0x3fff - 100;
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(1, maxChunkSize))));
        }
        return list;
    }

    private ByteBuf merge(List<ByteBuf> list) {
        ByteBuf merged = Unpooled.buffer();
        for (ByteBuf buf : list) {
            merged.writeBytes(buf);
        }
        return merged;
    }

    Session newContext(Mode mode, CipherKind kind, Socks5CommandRequest request) {
        return new Session(mode, new Identity(kind), request, ServerUserManager.EMPTY, new Context());
    }
}