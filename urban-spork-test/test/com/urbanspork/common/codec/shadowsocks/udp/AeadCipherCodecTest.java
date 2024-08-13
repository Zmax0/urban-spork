package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.template.TraceLevelLoggerTestTemplate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Base64;

@DisplayName("udp.AeadCipherCodecTest")
class AeadCipherCodecTest extends TraceLevelLoggerTestTemplate {

    @Test
    void testIncorrectPassword() {
        String password = Base64.getEncoder().encodeToString(Dice.rollBytes(10));
        ServerConfig config = new ServerConfig();
        config.setPassword(password);
        config.setCipher(CipherKind.aead2022_blake3_aes_128_gcm);
        Assertions.assertThrows(IllegalArgumentException.class, () -> AeadCipherCodecs.get(config));
    }

    @Test
    void testTooShortHeader() {
        AeadCipherCodec codec = newAEADCipherCodec();
        ByteBuf in = Unpooled.wrappedBuffer(Dice.rollBytes(3));
        Context context = new Context(Mode.Client, new Control(TestDice.rollCipher()), null, ServerUserManager.empty());
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(context, in));
    }

    @Test
    void testEmptyMsg() throws InvalidCipherTextException {
        testEmptyMsg(Mode.Client, Mode.Server);
        testEmptyMsg(Mode.Server, Mode.Client);
    }

    void testEmptyMsg(Mode from, Mode to) throws InvalidCipherTextException {
        AeadCipherCodec codec = newAEADCipherCodec();
        InetSocketAddress address = InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort());
        ByteBuf in = Unpooled.buffer();
        CipherKind kind = TestDice.rollCipher();
        codec.encode(new Context(from, new Control(kind), address, ServerUserManager.empty()), Unpooled.EMPTY_BUFFER, in);
        Assertions.assertTrue(in.isReadable());
        RelayingPacket<ByteBuf> pocket = codec.decode(new Context(to, new Control(kind), address, ServerUserManager.empty()), in);
        Assertions.assertFalse(in.isReadable());
        Assertions.assertNotNull(pocket);
    }

    @Test
    void testTooShortPacket() {
        AeadCipherCodec codec = newAEADCipherCodec();
        ByteBuf in = Unpooled.buffer();
        Context c1 = new Context(Mode.Client, new Control(CipherKind.aead2022_blake3_aes_128_gcm), null, ServerUserManager.empty());
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c1, in));
        Context c2 = new Context(Mode.Server, new Control(CipherKind.aead2022_blake3_aes_128_gcm), null, ServerUserManager.empty());
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c2, in));
    }

    @Test
    void testInvalidSocketType() throws InvalidCipherTextException {
        InetSocketAddress address = InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort());
        Context c1 = new Context(Mode.Client, new Control(CipherKind.aead2022_blake3_aes_128_gcm), address, ServerUserManager.empty());
        testInvalidSocketType(c1);
        Context c2 = new Context(Mode.Server, new Control(CipherKind.aead2022_blake3_aes_128_gcm), address, ServerUserManager.empty());
        testInvalidSocketType(c2);
    }

    private static void testInvalidSocketType(Context c) throws InvalidCipherTextException {
        AeadCipherCodec codec = newAEADCipherCodec();
        byte[] msg = Dice.rollBytes(10);
        ByteBuf in = Unpooled.buffer();
        in.writeBytes(msg);
        ByteBuf out = Unpooled.buffer();
        codec.encode(c, in, out);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c, out));
    }

    static AeadCipherCodec newAEADCipherCodec() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocol.shadowsocks, kind));
        config.setCipher(kind);
        return AeadCipherCodecs.get(config);
    }

    @Override
    protected Class<?> loggerClass() {
        return AeadCipherCodec.class;
    }
}