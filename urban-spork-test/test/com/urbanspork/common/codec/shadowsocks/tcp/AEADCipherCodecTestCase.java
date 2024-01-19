package com.urbanspork.common.codec.shadowsocks.tcp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Session;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.template.TraceLevelLoggerTestTemplate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;

@DisplayName("Shadowsocks - AEAD Cipher TCP Codec")
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
    void testUnexpectedStreamType() throws InvalidCipherTextException {
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, TestDice.rollHost(), TestDice.rollPort());
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        int saltSize = 16;
        String password = TestDice.rollPassword(Protocols.shadowsocks, kind);
        CipherMethod method = CipherMethods.AES_GCM.get();
        ServerConfig config = new ServerConfig();
        config.setPassword(password);
        config.setCipher(kind);
        AeadCipherCodec codec = AeadCipherCodecs.get(config);
        ByteBuf msg = Unpooled.buffer();
        codec.encode(new Context(Mode.Client, new Session(kind), request, ServerUserManager.EMPTY), Unpooled.wrappedBuffer(Dice.rollBytes(10)), msg);
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
        Context context = new Context(Mode.Server, new Session(kind), request, ServerUserManager.EMPTY);
        codec.decode(context, msg, out);
        Assertions.assertThrows(DecoderException.class, () -> codec.decode(context, temp, out));
    }

    @Override
    protected Logger logger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(AeadCipherCodec.class);
    }
}