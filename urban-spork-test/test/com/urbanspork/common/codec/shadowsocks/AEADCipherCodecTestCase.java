package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestContext;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - AEAD Cipher Codec")
@TestInstance(Lifecycle.PER_CLASS)
class AEADCipherCodecTestCase {

    private byte[] in;
    private CipherKind kind;
    private String password;

    @BeforeAll
    void beforeAll() {
        in = Dice.rollBytes(0xffff * 10);
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
        this.password = TestDice.rollPassword(Protocols.shadowsocks, kind);
        this.kind = kind;
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, host, port);
        cipherTest(new RequestContext(Network.TCP, StreamType.Request, request), new RequestContext(Network.TCP, StreamType.Response, null), true);
        cipherTest(new RequestContext(Network.UDP, StreamType.Request, request), new RequestContext(Network.UDP, StreamType.Response, null), false);
    }


    private void cipherTest(RequestContext request, RequestContext response, boolean reRoll) throws Exception {
        List<Object> list = cipherTest(request, response, Unpooled.copiedBuffer(in), reRoll);
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

    private List<Object> cipherTest(RequestContext request, RequestContext response, ByteBuf in, boolean reRoll) throws Exception {
        AEADCipherCodec client = AEADCipherCodecs.get(kind, password);
        AEADCipherCodec server = AEADCipherCodecs.get(kind, password);
        List<ByteBuf> encodeSlices = new ArrayList<>();
        for (ByteBuf slice : randomSlice(in, false)) {
            ByteBuf buf = Unpooled.buffer();
            client.encode(request, slice, buf);
            encodeSlices.add(buf);
        }
        List<Object> out = new ArrayList<>();
        if (reRoll) {
            ByteBuf buffer = Unpooled.buffer();
            for (ByteBuf slice : randomSlice(merge(encodeSlices), true)) {
                buffer.writeBytes(slice);
                server.decode(response, buffer, out);
            }
        } else {
            for (ByteBuf buf : encodeSlices) {
                server.decode(response, buf, out);
            }
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
}