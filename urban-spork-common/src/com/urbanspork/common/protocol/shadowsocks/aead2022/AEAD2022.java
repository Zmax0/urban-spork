package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.EmptyPaddingLengthGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.chunk.AEADChunkSizeParser;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.crypto.AES;
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import com.urbanspork.common.util.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.params.Blake3Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AEAD-2022 Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/sip022.html>SIP022 AEAD-2022 Ciphers</a>
 */
public interface AEAD2022 {

    Logger logger = LoggerFactory.getLogger(AEAD2022.class);
    int MIN_PADDING_LENGTH = 0;
    int MAX_PADDING_LENGTH = 900;
    long SERVER_STREAM_TIMESTAMP_MAX_DIFF = 30;

    interface TCP {
        // TCP
        /*
            +----------------+
            |  length chunk  |
            +----------------+
            | u16 big-endian |
            +----------------+

            +---------------+
            | payload chunk |
            +---------------+
            |   variable    |
            +---------------+

            Request stream:
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+
            |  salt  | encrypted header chunk |  encrypted header chunk   | encrypted length chunk |  encrypted payload chunk  |...|
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+
            | 16/32B |     11B + 16B tag      | variable length + 16B tag |  2B length + 16B tag   | variable length + 16B tag |...|
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+

            Response stream:
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+
            |  salt  | encrypted header chunk |  encrypted payload chunk  | encrypted length chunk |  encrypted payload chunk  |...|
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+
            | 16/32B |    27/43B + 16B tag    | variable length + 16B tag |  2B length + 16B tag   | variable length + 16B tag |...|
            +--------+------------------------+---------------------------+------------------------+---------------------------+---+
        */

        /*
            Request fixed-length header:
            +------+------------------+--------+
            | type |     timestamp    | length |
            +------+------------------+--------+
            |  1B  | u64be unix epoch |  u16be |
            +------+------------------+--------+

            Request variable-length header:
            +------+----------+-------+----------------+----------+-----------------+
            | ATYP |  address |  port | padding length |  padding | initial payload |
            +------+----------+-------+----------------+----------+-----------------+
            |  1B  | variable | u16be |     u16be      | variable |    variable     |
            +------+----------+-------+----------------+----------+-----------------+
        */

        /*
            Response fixed-length header:
            +------+------------------+----------------+--------+
            | type |     timestamp    |  request salt  | length |
            +------+------------------+----------------+--------+
            |  1B  | u64be unix epoch |     16/32B     |  u16be |
            +------+------------------+----------------+--------+

            Request variable-length header:
            +------+----------+-------+----------------+----------+-----------------+
            | ATYP |  address |  port | padding length |  padding | initial payload |
            +------+----------+-------+----------------+----------+-----------------+
            |  1B  | variable | u16be |     u16be      | variable |    variable     |
            +------+----------+-------+----------------+----------+-----------------+
        */

        static byte[][] newHeader(Mode mode, byte[] requestSalt, ByteBuf msg) {
            int saltLength = 0;
            if (requestSalt != null) {
                saltLength = requestSalt.length;
            }
            ByteBuf fixed = Unpooled.wrappedBuffer(new byte[1 + 8 + saltLength + 2]);
            fixed.setByte(0, mode.getValue());
            fixed.setLong(1, AEAD2022.newTimestamp());
            if (requestSalt != null) {
                fixed.setBytes(1 + 8, requestSalt);
            }
            int length = Math.min(msg.readableBytes(), 0xffff);
            byte[] via = new byte[length];
            msg.readBytes(via);
            fixed.setShort(1 + 8 + saltLength, length);
            byte[][] res = new byte[2][];
            res[0] = fixed.array();
            res[1] = via;
            return res;
        }

        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(sessionSubkey(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0xffff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(sessionSubkey(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, Identity identity, ServerUserManager userManager, byte[] key, byte[] salt, byte[] eih) {
            byte[] identitySubKey = deriveKey("shadowsocks 2022 identity subkey".getBytes(), concat(key, salt));
            byte[] userHash = AES.decrypt(identitySubKey, eih, cipherMethod.keySize());
            if (logger.isTraceEnabled()) {
                logger.trace("server EIH {}, hash: {}", ByteString.valueOf(eih), ByteString.valueOf(userHash));
            }
            ServerUser user = userManager.getUserByHash(userHash);
            if (user == null) {
                throw new DecoderException("invalid client user identity " + ByteString.valueOf(userHash));
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("{} chosen by EIH", user);
                }
                identity.setUser(user);
                return newPayloadDecoder(cipherMethod, user.key(), salt);
            }
        }

        // session_subkey := blake3::derive_key(context: "shadowsocks 2022 session subkey", key_material: key + salt)
        static byte[] sessionSubkey(byte[] key, byte[] salt) {
            return deriveKey("shadowsocks 2022 session subkey".getBytes(), concat(key, salt));
        }

        static void withEih(CipherKind kind, Keys keys, byte[] salt, ByteBuf out) {
            byte[] subKey = null;
            for (byte[] iPSK : keys.identityKeys()) {
                if (subKey != null) {
                    withEih(kind, subKey, iPSK, out);
                }
                subKey = deriveKey("shadowsocks 2022 identity subkey".getBytes(), concat(iPSK, salt));
            }
            if (subKey != null) {
                withEih(kind, subKey, keys.encKey(), out);
            }
        }

        private static void withEih(CipherKind kind, byte[] subKey, byte[] iPSK, ByteBuf out) {
            byte[] iPSKHash = Digests.blake3.hash(iPSK);
            byte[] iPSKPlainText = Arrays.copyOf(iPSKHash, 16);
            byte[] iPSKPEncryptText = AES.encrypt(subKey, iPSKPlainText, kind.keySize());
            if (logger.isTraceEnabled()) {
                logger.trace("client EIH:{}, hash:{}", ByteString.valueOf(iPSKPEncryptText), ByteString.valueOf(iPSKPlainText));
            }
            out.writeBytes(iPSKPEncryptText);
        }
    }

    interface UDP {
        // UDP
        /*
            Packet:
            +---------------------------+---------------------------+
            | encrypted separate header |       encrypted body      |
            +---------------------------+---------------------------+
            |            16B            | variable length + 16B tag |
            +---------------------------+---------------------------+
        */

        /*
            Separate header:
            +------------+-----------+
            | session ID | packet ID |
            +------------+-----------+
            |     8B     |   u64be   |
            +------------+-----------+

            Client-to-server message header:
            +------+------------------+----------------+----------+------+----------+-------+
            | type |     timestamp    | padding length |  padding | ATYP |  address |  port |
            +------+------------------+----------------+----------+------+----------+-------+
            |  1B  | u64be unix epoch |     u16be      | variable |  1B  | variable | u16be |
            +------+------------------+----------------+----------+------+----------+-------+

            Server-to-client message header:
            +------+------------------+-------------------+----------------+----------+------+----------+-------+
            | type |     timestamp    | client session ID | padding length |  padding | ATYP |  address |  port |
            +------+------------------+-------------------+----------------+----------+------+----------+-------+
            |  1B  | u64be unix epoch |         8B        |     u16be      | variable |  1B  | variable | u16be |
            +------+------------------+-------------------+----------------+----------+------+----------+-------+
        */
        Duration CIPHER_CACHE_DURATION = Duration.ofSeconds(30);
        int CIPHER_CACHE_LIMIT = 102400;

        static int getNonceLength(CipherKind kind) {
            if (CipherKind.aead2022_blake3_aes_128_gcm == kind || CipherKind.aead2022_blake3_aes_256_gcm == kind) {
                return 0;
            }
            if (CipherKind.aead2022_blake3_chacha20_poly1305 == kind) {
                return 24;
            }
            throw new IllegalArgumentException(kind + " is not an AEAD 2022 cipher");
        }

        static UdpCipher getCipher(CipherKind kind, CipherMethod method, byte[] key, long sessionId) {
            return UdpCipherCache.INSTANCE.get(kind, method, key, sessionId);
        }

        static void encodePacket(UdpCipher cipher, byte[] iPSK, int eihLength, ByteBuf in, ByteBuf out) throws InvalidCipherTextException {
            byte[] header = new byte[16];
            in.readBytes(header);
            byte[] nonce = Arrays.copyOfRange(header, 4, 16);
            AES.encrypt(iPSK, header, cipher.method().keySize(), header);
            out.writeBytes(header);
            if (eihLength > 0) {
                in.readBytes(out, eihLength);
            }
            byte[] encrypting = new byte[in.readableBytes()];
            in.readBytes(encrypting);
            out.writeBytes(cipher.seal(encrypting, nonce));
        }

        static ByteBuf decodePacket(CipherKind kind, CipherMethod method, Control control, ServerUserManager userManager, byte[] key, ByteBuf in) throws InvalidCipherTextException {
            byte[] header = new byte[16];
            in.readBytes(header);
            AES.decrypt(key, header, method.keySize(), header);
            ByteBuf headerBuffer = Unpooled.wrappedBuffer(header);
            long sessionId = headerBuffer.getLong(0);
            UdpCipher cipher;
            byte[] eih;
            if (kind.supportEih() && userManager.userCount() > 0) {
                eih = new byte[16];
                in.readBytes(eih);
                if (logger.isTraceEnabled()) {
                    logger.trace("server EIH {}, session_id_packet_id: {},{}", ByteString.valueOf(eih), sessionId, headerBuffer.getLong(Long.BYTES));
                }
                AES.decrypt(key, eih, method.keySize(), eih);
                for (int i = 0; i < eih.length; i++) {
                    eih[i] ^= header[i];
                }
                ServerUser user = userManager.getUserByHash(eih);
                if (user == null) {
                    throw new DecoderException("user with identity " + ByteString.valueOf(eih) + " not found");
                } else {
                    logger.trace("{} chosen by EIH", user);
                    cipher = getCipher(kind, method, user.key(), sessionId);
                    control.setUser(user);
                }
            } else {
                eih = new byte[0];
                cipher = getCipher(kind, method, key, sessionId);
            }
            byte[] nonce = new byte[12];
            headerBuffer.getBytes(4, nonce);
            byte[] decrypting = new byte[in.readableBytes()];
            in.readBytes(decrypting);
            return Unpooled.wrappedBuffer(header, eih, cipher.open(decrypting, nonce));
        }

        static byte[] sessionSubkey(byte[] key, long sessionId) {
            byte[] in = new byte[key.length + Long.BYTES];
            System.arraycopy(key, 0, in, 0, key.length);
            Unpooled.wrappedBuffer(in).setLong(key.length, sessionId);
            return deriveKey("shadowsocks 2022 session subkey".getBytes(), in);
        }

        static void withEih(CipherKind kind, byte[] key, byte[][] identityKeys, byte[] sessionIdPacketId, ByteBuf buffer) {
            for (int i = 0; i < identityKeys.length; i++) {
                byte[] identityHeader = new byte[16];
                if (i != identityKeys.length - 1) {
                    withEih(kind, identityKeys[i], identityKeys[i + 1], sessionIdPacketId, identityHeader);
                } else {
                    withEih(kind, identityKeys[i], key, sessionIdPacketId, identityHeader);
                }
                buffer.writeBytes(identityHeader);
            }
        }

        private static void withEih(CipherKind kind, byte[] iPSK, byte[] iPSKn, byte[] sessionIdPacketId, byte[] identityHeader) {
            byte[] iPSKnHash = Digests.blake3.hash(iPSKn);
            byte[] iPSKnHashPlainText = Arrays.copyOf(iPSKnHash, 16);
            System.arraycopy(iPSKnHashPlainText, 0, identityHeader, 0, iPSKnHashPlainText.length);
            for (int i = 0; i < 16; i++) {
                identityHeader[i] ^= sessionIdPacketId[i];
            }
            AES.encrypt(iPSK, identityHeader, kind.keySize(), identityHeader);
            if (logger.isTraceEnabled()) {
                logger.trace("client EIH:{}, hash:{}", ByteString.valueOf(identityHeader), ByteString.valueOf(iPSKnHashPlainText));
            }
        }
    }

    static long newTimestamp() {
        return Instant.now().getEpochSecond();
    }

    static void validateTimestamp(long timestamp) {
        long now = AEAD2022.newTimestamp();
        long diff = timestamp - now;
        if (Math.abs(diff) > AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF) {
            String msg = String.format("invalid timestamp %d - now %d = %d", timestamp, now, diff);
            throw new DecoderException(msg);
        }
    }

    static int getPaddingLength(ByteBuf msg) {
        if (msg.isReadable()) {
            return 0;
        } else {
            return ThreadLocalRandom.current().nextInt(AEAD2022.MIN_PADDING_LENGTH, AEAD2022.MAX_PADDING_LENGTH) + 1;
        }
    }

    static byte[] concat(byte[] bytes1, byte[] bytes2) {
        byte[] result = Arrays.copyOf(bytes1, bytes1.length + bytes2.length);
        System.arraycopy(bytes2, 0, result, bytes1.length, bytes2.length);
        return result;
    }

    private static byte[] deriveKey(byte[] context, byte[] keyMaterial) {
        Blake3Digest digest = new Blake3Digest();
        Blake3Parameters parameters = Blake3Parameters.context(context);
        digest.init(parameters);
        digest.update(keyMaterial, 0, keyMaterial.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    static Keys passwordToKeys(String password) {
        // iPSK1:iPSK2:iPSK3:...:uPSK
        String[] split = password.split(":");
        String uPSK = split[split.length - 1];
        byte[] encKey = Base64.getDecoder().decode(uPSK.getBytes());
        byte[][] identityKeys = new byte[split.length - 1][];
        for (int i = 0; i < split.length - 1; i++) {
            identityKeys[i] = Base64.getDecoder().decode(split[i]);
        }
        return new Keys(encKey, identityKeys);
    }
}