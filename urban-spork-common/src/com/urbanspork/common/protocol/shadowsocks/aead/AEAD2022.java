package com.urbanspork.common.protocol.shadowsocks.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.EmptyPaddingLengthGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.chunk.AEADChunkSizeParser;
import com.urbanspork.common.codec.chunk.EmptyChunkSizeParser;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.params.Blake3Parameters;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AEAD-2022 Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/sip022.html>SIP022 AEAD-2022 Ciphers</a>
 */
public interface AEAD2022 {
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

        static byte[][] newRequestHeader(ByteBuf msg) {
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
            return newHeader(StreamType.Request, null, msg);
        }

        static byte[][] newResponseHeader(byte[] requestSalt, ByteBuf msg) {
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
            return newHeader(StreamType.Response, requestSalt, msg);
        }

        private static byte[][] newHeader(StreamType streamType, byte[] requestSalt, ByteBuf msg) {
            int saltLength = 0;
            if (requestSalt != null) {
                saltLength = requestSalt.length;
            }
            ByteBuf fixed = Unpooled.wrappedBuffer(new byte[1 + 8 + saltLength + 2]);
            fixed.setByte(0, streamType.getValue());
            fixed.setLong(1, AEAD2022.now());
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
            Authenticator auth = new Authenticator(cipherMethod, sessionSubkey(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0xffff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(cipherMethod, sessionSubkey(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }

        // session_subkey := blake3::derive_key(context: "shadowsocks 2022 session subkey", key_material: key + salt)
        static byte[] sessionSubkey(byte[] key, byte[] salt) {
            byte[] in = new byte[key.length + salt.length];
            System.arraycopy(key, 0, in, 0, key.length);
            System.arraycopy(salt, 0, in, key.length, salt.length);
            return deriveKey("shadowsocks 2022 session subkey".getBytes(), in);
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
        static int getNonceLength(CipherKind kind) {
            if (CipherKind.aead2022_blake3_aes_128_gcm == kind || CipherKind.aead2022_blake3_aes_256_gcm == kind) {
                return 0;
            }
            throw new IllegalArgumentException(kind + " is not an AEAD 2022 cipher");
        }

        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, long sessionId, byte[] nonce) {
            Authenticator auth = new Authenticator(cipherMethod, sessionSubkey(key, sessionId), NonceGenerator.generateStaticNonce(nonce), BytesGenerator.generateEmptyBytes());
            return new PayloadEncoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE, 0xffff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, long sessionId, byte[] nonce) {
            Authenticator auth = new Authenticator(cipherMethod, sessionSubkey(key, sessionId), NonceGenerator.generateStaticNonce(nonce), BytesGenerator.generateEmptyBytes());
            return new PayloadDecoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE);
        }

        static byte[] sessionSubkey(byte[] key, long sessionId) {
            byte[] in = new byte[key.length + Long.BYTES];
            System.arraycopy(key, 0, in, 0, key.length);
            Unpooled.wrappedBuffer(in).setLong(key.length, sessionId);
            return deriveKey("shadowsocks 2022 session subkey".getBytes(), in);
        }
    }

    static long now() {
        return Instant.now().getEpochSecond();
    }

    static int getPaddingLength(ByteBuf msg) {
        if (msg.isReadable()) {
            return 0;
        } else {
            return ThreadLocalRandom.current().nextInt(AEAD2022.MIN_PADDING_LENGTH, AEAD2022.MAX_PADDING_LENGTH) + 1;
        }
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
}