package com.urbanspork.common.codec.shadowsocks.udp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Control;
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
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@DisplayName("Shadowsocks - AEAD Cipher UDP Codec")
class AEADCipherCodecTestCase extends TraceLevelLoggerTestTemplate {

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
        List<Object> out = new ArrayList<>();
        ByteBuf in = Unpooled.wrappedBuffer(Dice.rollBytes(3));
        Context context = new Context(Mode.Client, new Control(TestDice.rollCipher()), null, ServerUserManager.EMPTY);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(context, in, out));
    }

    @Test
    void testEmptyMsg() throws InvalidCipherTextException {
        AeadCipherCodec codec = newAEADCipherCodec();
        InetSocketAddress address = InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort());
        List<Object> out = new ArrayList<>();
        ByteBuf in = Unpooled.buffer();
        CipherKind kind = TestDice.rollCipher();
        codec.encode(new Context(Mode.Client, new Control(kind), address, ServerUserManager.EMPTY), Unpooled.EMPTY_BUFFER, in);
        Assertions.assertTrue(in.isReadable());
        codec.decode(new Context(Mode.Server, new Control(kind), address, ServerUserManager.EMPTY), in, out);
        Assertions.assertFalse(in.isReadable());
        Assertions.assertFalse(out.isEmpty());
    }

    @Test
    void testTooShortPacket() {
        AeadCipherCodec codec = newAEADCipherCodec();
        ByteBuf in = Unpooled.buffer();
        List<Object> out = new ArrayList<>();
        Context c1 = new Context(Mode.Client, new Control(CipherKind.aead2022_blake3_aes_128_gcm), null, ServerUserManager.EMPTY);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c1, in, out));
        Context c2 = new Context(Mode.Server, new Control(CipherKind.aead2022_blake3_aes_128_gcm), null, ServerUserManager.EMPTY);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c2, in, out));
    }

    @Test
    void testInvalidSocketType() throws InvalidCipherTextException {
        InetSocketAddress address = InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort());
        Context c1 = new Context(Mode.Client, new Control(CipherKind.aead2022_blake3_aes_128_gcm), address, ServerUserManager.EMPTY);
        testInvalidSocketType(c1);
        Context c2 = new Context(Mode.Server, new Control(CipherKind.aead2022_blake3_aes_128_gcm), address, ServerUserManager.EMPTY);
        testInvalidSocketType(c2);
    }

    private static void testInvalidSocketType(Context c) throws InvalidCipherTextException {
        AeadCipherCodec codec = newAEADCipherCodec();
        byte[] msg = Dice.rollBytes(10);
        ByteBuf in = Unpooled.buffer();
        in.writeBytes(msg);
        ByteBuf out = Unpooled.buffer();
        codec.encode(c, in, out);
        ArrayList<Object> list = new ArrayList<>();
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(c, out, list));
    }

    static AeadCipherCodec newAEADCipherCodec() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocols.shadowsocks, kind));
        config.setCipher(kind);
        return AeadCipherCodecs.get(config);
    }

    @Override
    protected Logger logger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(AeadCipherCodec.class);
    }
}