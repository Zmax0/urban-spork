package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - Cipher Codec")
@TestInstance(Lifecycle.PER_CLASS)
class CipherCodecTestCase {

    private String password;
    private byte[] in;
    private final int maxChunkSize = 0xffff;

    @BeforeAll
    void beforeAll() {
        password = TestDice.randomString();
        SecureRandom random = new SecureRandom();
        in = new byte[maxChunkSize * 10]; // 1M
        random.nextBytes(in);
    }

    @DisplayName("Single supported cipher repeat")
    @RepeatedTest(10)
    void repeatedTest() throws Exception {
        cipherTest(ShadowsocksAEADCipherCodecs.get(password, SupportedCipher.aes_128_gcm, Network.TCP));
        cipherTest(ShadowsocksAEADCipherCodecs.get(password, SupportedCipher.aes_128_gcm, Network.UDP));
    }

    @ParameterizedTest
    @DisplayName("All supported cipher iterate")
    @EnumSource(SupportedCipher.class)
    void parameterizedTest(SupportedCipher cipher) throws Exception {
        cipherTest(ShadowsocksAEADCipherCodecs.get(password, cipher, Network.TCP));
        cipherTest(ShadowsocksAEADCipherCodecs.get(password, cipher, Network.UDP));
    }

    private void cipherTest(ShadowsocksAEADCipherCodec codec) throws Exception {
        List<Object> list = cipherTest(codec, Unpooled.copiedBuffer(in));
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
        Assertions.assertArrayEquals(in, out);
    }

    private List<Object> cipherTest(ShadowsocksAEADCipherCodec codec, ByteBuf inBuf) throws Exception {
        ByteBuf trans = Unpooled.buffer();
        ChannelHandlerContext ctx = new EmbeddedChannel().pipeline().firstContext();
        for (ByteBuf slice : randomSlice(inBuf)) {
            codec.encode(ctx, slice, trans);
        }
        List<Object> out = new ArrayList<>();
        ByteBuf buffer = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(trans)) {
            buffer.writeBytes(slice);
            codec.decode(ctx, buffer, out);
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        while (src.isReadable()) {
            list.add(src.readSlice(Math.max(src.readableBytes(), random.nextInt(1, maxChunkSize))));
        }
        return list;
    }

}