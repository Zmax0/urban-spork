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
import com.urbanspork.common.crypto.Blake3;
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.protocol.shadowsocks.Identity;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
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
            Authenticator auth = new Authenticator(sessionSubkey(cipherMethod, key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0xffff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(sessionSubkey(cipherMethod, key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, Identity identity, ServerUserManager userManager, byte[] key, byte[] salt, byte[] eih) {
            byte[] identitySubKey = identitySubkey(cipherMethod, key, salt);
            String encryptedEih = logger.isTraceEnabled() ? ByteString.valueOf(eih) : null;
            aesDecrypt(cipherMethod.keySize(), identitySubKey, eih);
            if (logger.isTraceEnabled()) {
                logger.trace("server EIH {}, hash: {}", encryptedEih, ByteString.valueOf(eih));
            }
            ServerUser user = userManager.getUserByHash(eih);
            if (user == null) {
                throw new DecoderException("invalid client user identity " + ByteString.valueOf(eih));
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("{} chosen by EIH", user);
                }
                identity.setUser(user);
                return newPayloadDecoder(cipherMethod, user.key(), salt);
            }
        }

        // session_subkey := blake3::derive_key(context: "shadowsocks 2022 session subkey", key_material: key + salt)
        static byte[] sessionSubkey(CipherMethod method, byte[] key, byte[] salt) {
            return deriveKey(method.keySize(), "shadowsocks 2022 session subkey".getBytes(), Bytes.concat(key, salt));
        }

        // identity_subkey := blake3::derive_key(context: "shadowsocks 2022 identity subkey", key_material: key + salt)
        static byte[] identitySubkey(CipherMethod method, byte[] key, byte[] salt) {
            return deriveKey(method.keySize(), "shadowsocks 2022 identity subkey".getBytes(), Bytes.concat(key, salt));
        }

        static void withEih(CipherKind kind, Keys keys, byte[] salt, ByteBuf out) {
            byte[] subKey = null;
            for (byte[] iPSK : keys.identityKeys()) {
                if (subKey != null) {
                    withEih(kind, subKey, iPSK, out);
                }
                subKey = deriveKey(kind.keySize(), "shadowsocks 2022 identity subkey".getBytes(), Bytes.concat(iPSK, salt));
            }
            if (subKey != null) {
                withEih(kind, subKey, keys.encKey(), out);
            }
        }

        private static void withEih(CipherKind kind, byte[] subKey, byte[] iPSK, ByteBuf out) {
            byte[] iPSKHash = Digests.blake3.hash(iPSK);
            byte[] iPSKPlainText = Arrays.copyOf(iPSKHash, 16);
            String hash = logger.isTraceEnabled() ? ByteString.valueOf(iPSKPlainText) : null;
            aesEncrypt(kind.keySize(), subKey, iPSKPlainText);
            if (logger.isTraceEnabled()) {
                logger.trace("client EIH:{}, hash:{}", ByteString.valueOf(iPSKPlainText), hash);
            }
            out.writeBytes(iPSKPlainText);
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
            if (CipherKind.aead2022_blake3_chacha8_poly1305 == kind || CipherKind.aead2022_blake3_chacha20_poly1305 == kind) {
                return 24;
            }
            throw new IllegalArgumentException(kind + " is not an AEAD 2022 cipher");
        }

        static UdpCipher getCipher(CipherKind kind, CipherMethod method, byte[] key, long sessionId) {
            return UdpCipherCache.INSTANCE.get(kind, method, key, sessionId);
        }

        static void encodePacket(CipherKind kind, UdpCipher cipher, byte[] key, int eihLength, ByteBuf in, ByteBuf out) throws Exception {
            if (CipherKind.aead2022_blake3_chacha8_poly1305 == kind || CipherKind.aead2022_blake3_chacha20_poly1305 == kind) {
                byte[] nonce = new byte[getNonceLength(kind)];
                in.readBytes(nonce);
                byte[] buffer = newEncryptingBuffer(cipher, in);
                cipher.sealInPlace(buffer, nonce);
                out.writeBytes(nonce);
                out.writeBytes(buffer);
            } else {
                byte[] header = new byte[16];
                in.readBytes(header);
                byte[] nonce = Arrays.copyOfRange(header, 4, 16);
                aesEncrypt(kind.keySize(), key, header);
                out.writeBytes(header);
                if (eihLength > 0) {
                    in.readBytes(out, eihLength);
                }
                byte[] buffer = newEncryptingBuffer(cipher, in);
                cipher.sealInPlace(buffer, nonce);
                out.writeBytes(buffer);
            }
        }

        private static byte[] newEncryptingBuffer(UdpCipher cipher, ByteBuf in) {
            int length = in.readableBytes();
            byte[] buffer = new byte[length + cipher.overhead()];
            in.readBytes(buffer, 0, length);
            return buffer;
        }

        static ByteBuf decodePacket(CipherKind kind, CipherMethod method, Control control, ServerUserManager userManager, byte[] key, ByteBuf in) throws Exception {
            if (CipherKind.aead2022_blake3_chacha8_poly1305 == kind || CipherKind.aead2022_blake3_chacha20_poly1305 == kind) {
                byte[] nonce = new byte[getNonceLength(kind)];
                in.readBytes(nonce);
                long sessionId = in.getLong(0);
                UdpCipher cipher = getCipher(kind, method, key, sessionId);
                byte[] buffer = new byte[in.readableBytes()];
                in.readBytes(buffer);
                int plainLength = cipher.openInPlace(buffer, nonce);
                return Unpooled.wrappedBuffer(buffer, 0, plainLength);
            } else {
                byte[] header = new byte[16];
                in.readBytes(header);
                aesDecrypt(kind.keySize(), key, header);
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
                    aesDecrypt(kind.keySize(), key, eih);
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
                byte[] buffer = new byte[in.readableBytes()];
                in.readBytes(buffer);
                int plainLength = cipher.openInPlace(buffer, nonce);
                return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(header, eih), Unpooled.wrappedBuffer(buffer, 0, plainLength));
            }
        }

        static byte[] sessionSubkey(CipherMethod method, byte[] key, long sessionId) {
            byte[] in = new byte[key.length + Long.BYTES];
            System.arraycopy(key, 0, in, 0, key.length);
            Unpooled.wrappedBuffer(in).setLong(key.length, sessionId);
            return deriveKey(method.keySize(), "shadowsocks 2022 session subkey".getBytes(), in);
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
            aesEncrypt(kind.keySize(), iPSK, identityHeader);
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

    private static byte[] deriveKey(int length, byte[] context, byte[] keyMaterial) {
        return Blake3.deriveKey(context, keyMaterial, length);
    }

    static void aesEncrypt(int keySize, byte[] key, byte[] block) {
        if (AES.BLOCK_SIZE == keySize) {
            AES.encrypt128(key, block);
        } else {
            AES.encrypt256(key, block);
        }
    }

    static void aesDecrypt(int keySize, byte[] key, byte[] block) {
        if (AES.BLOCK_SIZE == keySize) {
            AES.decrypt128(key, block);
        } else {
            AES.decrypt256(key, block);
        }
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