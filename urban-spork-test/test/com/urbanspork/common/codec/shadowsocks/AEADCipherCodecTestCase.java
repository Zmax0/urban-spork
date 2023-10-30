package com.urbanspork.common.codec.shadowsocks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestContext;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD2022;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AEADCipherCodecTestCase {

    private Level raw;

    @BeforeAll
    void beforeAll() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(AEADCipherCodec.class);
        raw = logger.getEffectiveLevel();
        logger.setLevel(Level.TRACE);
    }

    @Test
    void testIncorrectPassword() {
        String password = TestDice.rollString(10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> AEADCipherCodecs.get(CipherKind.aead2022_blake3_aes_128_gcm, password));
    }

    @Test
    void testTooShortHeader() {
        AEADCipherCodec codec = newAEADCipherCodec();
        List<Object> out = new ArrayList<>();
        ByteBuf in = Unpooled.wrappedBuffer(Dice.rollBytes(3));
        RequestContext context = new RequestContext(Network.UDP, StreamType.Request);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(context, in, out));
    }

    @Test
    void testEmptyMsg() throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        AEADCipherCodec codec = newAEADCipherCodec();
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, TestDice.rollHost(), TestDice.rollPort());
        List<Object> out = new ArrayList<>();
        ByteBuf in = Unpooled.buffer();
        codec.encode(new RequestContext(Network.UDP, StreamType.Request, request), Unpooled.EMPTY_BUFFER, in);
        Assertions.assertTrue(in.isReadable());
        codec.decode(new RequestContext(Network.UDP, StreamType.Response, request), in, out);
        Assertions.assertFalse(in.isReadable());
        Assertions.assertFalse(out.isEmpty());
    }

    @Test
    void testUnexpectedStreamType() throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, TestDice.rollHost(), TestDice.rollPort());
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        int saltSize = 16;
        String password = TestDice.rollPassword(Protocols.shadowsocks, kind);
        CipherMethod method = CipherMethods.AES_GCM.get();
        AEADCipherCodec codec = new AEADCipherCodec(kind, method, password, saltSize);
        ByteBuf msg = Unpooled.buffer();
        codec.encode(new RequestContext(Network.TCP, StreamType.Request, request), Unpooled.wrappedBuffer(Dice.rollBytes(10)), msg);
        byte[] salt = new byte[saltSize];
        msg.readBytes(salt);
        byte[] passwordBytes = Base64.getDecoder().decode(password);
        PayloadDecoder decoder = AEAD2022.TCP.newPayloadDecoder(method, passwordBytes, salt);
        byte[] decryptedHeader = new byte[1 + 8 + 2 + method.tagSize()];
        msg.readBytes(decryptedHeader);
        byte[] header = decoder.auth().open(decryptedHeader);
        header[0] = 1;
        PayloadEncoder encoder = AEAD2022.TCP.newPayloadEncoder(method, passwordBytes, salt);
        ByteBuf temp = Unpooled.buffer();
        temp.writeBytes(salt);
        temp.writeBytes(encoder.auth().seal(header));
        temp.writeBytes(msg);
        ArrayList<Object> out = new ArrayList<>();
        RequestContext context = new RequestContext(Network.TCP, StreamType.Response, request);
        codec.decode(context, msg, out);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(context, temp, out));
    }

    @AfterAll
    void afterAll() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(AEADCipherCodec.class);
        logger.setLevel(raw);
    }

    static AEADCipherCodec newAEADCipherCodec() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        return new AEADCipherCodec(kind, CipherMethods.AES_GCM.get(), TestDice.rollPassword(Protocols.shadowsocks, kind), 16);
    }
}