package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@DisplayName("tcp.AeadCipherCodecTest")
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
    void testUnexpectedStreamType() throws InvalidCipherTextException {
        InetSocketAddress request = InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort());
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        int saltSize = 16;
        String password = TestDice.rollPassword(Protocol.shadowsocks, kind);
        CipherMethod method = CipherMethods.AES_GCM.get();
        ServerConfig config = new ServerConfig();
        config.setPassword(password);
        config.setCipher(kind);
        AeadCipherCodec codec = AeadCipherCodecs.get(config);
        ByteBuf msg = Unpooled.buffer();
        codec.encode(new Session(Mode.Client, new Identity(kind), request, ServerUserManager.EMPTY, new Context()), Unpooled.wrappedBuffer(Dice.rollBytes(10)), msg);
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
        Session session = new Session(Mode.Server, new Identity(kind), request, ServerUserManager.EMPTY, new Context());
        codec.decode(session, msg, out);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(session, temp, out));
    }

    @Test
    void testAead2022TcpAntiReplay() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocol.shadowsocks, kind));
        config.setCipher(kind);
        Context context = Context.newCheckReplayInstance();
        InetSocketAddress request = InetSocketAddress.createUnresolved("localhost", 16800);
        ByteBuf msg1 = Unpooled.buffer();
        Identity identity = new Identity(kind);
        Session clientSession = new Session(Mode.Client, identity, request, ServerUserManager.EMPTY, context);
        AeadCipherCodec clientCodec = AeadCipherCodecs.get(config);
        Assertions.assertDoesNotThrow(() -> clientCodec.encode(clientSession, Unpooled.wrappedBuffer(Dice.rollBytes(10)), msg1));
        ByteBuf msg2 = msg1.copy();
        Assertions.assertTrue(msg1.isReadable());
        Assertions.assertTrue(msg2.isReadable());
        ByteBuf tooShortMsg = Unpooled.wrappedBuffer(Dice.rollBytes(33));
        Session serverSession = new Session(Mode.Server, identity, request, ServerUserManager.EMPTY, context);
        List<Object> out = new ArrayList<>();
        AeadCipherCodec serverCodec1 = AeadCipherCodecs.get(config);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> serverCodec1.decode(serverSession, tooShortMsg, out));
        Assertions.assertDoesNotThrow(() -> serverCodec1.decode(serverSession, msg1, out));
        AeadCipherCodec serverCodec2 = AeadCipherCodecs.get(config);
        Assertions.assertThrows(DecoderException.class, () -> serverCodec2.decode(serverSession, msg2, out));
    }

    @Override
    protected Class<?> loggerClass() {
        return AeadCipherCodec.class;
    }
}