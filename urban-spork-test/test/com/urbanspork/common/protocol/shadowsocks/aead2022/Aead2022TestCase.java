package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.Control;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - AEAD 2022")
class Aead2022TestCase extends TraceLevelLoggerTestTemplate {
    @Test
    void testSessionSubkey() {
        byte[] key = Base64.getDecoder().decode("Lc3tTx0BY6ZJ/fCwOx3JvF0I/anhwJBO5p2+FA5Vce4=");
        byte[] salt = Base64.getDecoder().decode("3oFO0VyLyGI4nFN0M9P+62vPND/L6v8IingaPJWTbJA=");
        byte[] bytes = AEAD2022.TCP.sessionSubkey(key, salt);
        Assertions.assertEquals("EdNE+4U8dVnHT0+poAFDK2bdlwfrHT61sUNr9WYPh+E=", Base64.getEncoder().encodeToString(bytes));
    }

    @Test
    void testValidateTimestamp() {
        long timestamp = AEAD2022.newTimestamp() + 2 * AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF;
        Assertions.assertThrows(DecoderException.class, () -> AEAD2022.validateTimestamp(timestamp));
    }

    @Test
    void testGetPaddingLength() {
        Assertions.assertTrue(AEAD2022.getPaddingLength(Unpooled.EMPTY_BUFFER) > 0);
    }

    @Test
    void testGetNonceLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AEAD2022.UDP.getNonceLength(CipherKind.aes_128_gcm));
    }

    @Test
    void testTcpWithEih() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        StringJoiner joiner = new StringJoiner(":");
        for (int i = 0; i < 3; i++) {
            joiner.add(TestDice.rollPassword(Protocols.shadowsocks, kind));
        }
        String password = joiner.toString();
        Keys keys = AEAD2022.passwordToKeys(password);
        byte[] salt = Dice.rollBytes(kind.keySize());
        ByteBuf out = Unpooled.buffer();
        AEAD2022.TCP.withEih(keys, salt, out);
        Assertions.assertTrue(out.isReadable());
    }

    @Test
    void testTcpInvalidClientUserIdentity() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        CipherMethod method = CipherMethods.AES_GCM.get();
        ServerUser user = rollUser(kind);
        ServerUserManager userManager = ServerUserManager.DEFAULT;
        userManager.addUser(user);
        byte[] key = Dice.rollBytes(kind.keySize());
        byte[] salt = Dice.rollBytes(kind.keySize());
        byte[] eih = Dice.rollBytes(16);
        Assertions.assertThrows(DecoderException.class, () -> AEAD2022.TCP.newPayloadDecoder(method, null, userManager, key, salt, eih));
        userManager.removeUserByHash(user.identityHash());
    }

    @Test
    void testUdpUserNotFound() throws InvalidCipherTextException {
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        CipherMethod method = CipherMethods.AES_GCM.get();
        byte[] iPSK = Base64.getDecoder().decode(TestDice.rollPassword(Protocols.shadowsocks, kind));
        ServerUser user = rollUser(kind);
        ServerUserManager userManager = ServerUserManager.DEFAULT;
        userManager.addUser(user);
        long sessionId = ThreadLocalRandom.current().nextLong();
        long packetId = ThreadLocalRandom.current().nextLong();
        String msg = TestDice.rollString(10);
        ByteBuf in = Unpooled.buffer();
        in.writeLong(sessionId);
        in.writeLong(packetId);
        in.writeBytes(user.identityHash());
        in.writeCharSequence(msg, StandardCharsets.US_ASCII);
        ByteBuf out = Unpooled.buffer();
        AEAD2022.UDP.encodePacket(AEAD2022.UDP.getCipher(kind, method, iPSK, sessionId), iPSK, 16, in, out);
        Control control = new Control(kind);
        Assertions.assertThrows(DecoderException.class, () -> AEAD2022.UDP.decodePacket(kind, method, control, userManager, iPSK, out));
        userManager.removeUserByHash(user.identityHash());
    }

    @Test
    void testUdpWithEih() {
        byte[] key = Dice.rollBytes(16);
        byte[][] identityKeys = new byte[3][];
        for (int i = 0; i < identityKeys.length; i++) {
            identityKeys[i] = Dice.rollBytes(16);
        }
        byte[] sessionIdPacketId = new byte[16];
        ByteBuf out = Unpooled.buffer();
        AEAD2022.UDP.withEih(key, identityKeys, sessionIdPacketId, out);
        Assertions.assertTrue(out.isReadable());
    }

    private static ServerUser rollUser(CipherKind kind) {
        return ServerUser.from(new ServerUserConfig("underdog", TestDice.rollPassword(Protocols.shadowsocks, kind)));
    }

    @Override
    protected Class<?> loggerClass() {
        return AEAD2022.class;
    }
}