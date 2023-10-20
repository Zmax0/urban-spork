package com.urbanspork.common.protocol.shadowsocks.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.EmptyPaddingLengthGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherCodec;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.chunk.AEADChunkSizeParser;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.params.Blake3Parameters;

import java.time.Instant;
import java.util.List;

public interface AEAD2022 {
    int MIN_PADDING_LENGTH = 0;
    int MAX_PADDING_LENGTH = 900;
    long SERVER_STREAM_TIMESTAMP_MAX_DIFF = 30;

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
        return newHeader(StreamType.Request, new byte[]{}, msg);
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
        ByteBuf fixed = Unpooled.wrappedBuffer(new byte[1 + 8 + requestSalt.length + 2]);
        fixed.writerIndex(0);
        fixed.writeByte(streamType.getValue());
        fixed.writeLong(AEAD2022.now());
        fixed.writeBytes(requestSalt);
        int length = Math.min(msg.readableBytes(), 0xffff);
        byte[] via = new byte[length];
        msg.readBytes(via);
        fixed.writeShort(length);
        byte[][] res = new byte[2][];
        res[0] = fixed.array();
        res[1] = via;
        return res;
    }

    static PayloadEncoder newPayloadEncoder(CipherCodec cipherCodec, byte[] secret, byte[] salt) {
        Authenticator auth = new Authenticator(cipherCodec, sessionSubkey(secret, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
        AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
        return new PayloadEncoder() {
            @Override
            public int payloadLimit() {
                return 0xffff;
            }

            @Override
            public Authenticator auth() {
                return auth;
            }

            @Override
            public ChunkSizeCodec sizeCodec() {
                return sizeCodec;
            }

            @Override
            public PaddingLengthGenerator padding() {
                return EmptyPaddingLengthGenerator.INSTANCE;
            }

            @Override
            public void encodePacket(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
                byte[] in = new byte[msg.readableBytes()];
                msg.readBytes(in);
                out.writeBytes(auth().seal(in));
            }
        };
    }

    static PayloadDecoder newPayloadDecoder(CipherCodec cipherCodec, byte[] secret, byte[] salt) {
        Authenticator auth = new Authenticator(cipherCodec, sessionSubkey(secret, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
        AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
        return new PayloadDecoder() {

            private int payloadLength = PayloadDecoder.INIT_LENGTH;

            @Override
            public int payloadLength() {
                return this.payloadLength;
            }

            @Override
            public void updatePayloadLength(int payloadLength) {
                this.payloadLength = payloadLength;
            }

            @Override
            public int paddingLength() {
                return 0;
            }

            @Override
            public void updatePaddingLength(int paddingLength) {
                // unsupported
            }

            @Override
            public Authenticator auth() {
                return auth;
            }

            @Override
            public ChunkSizeCodec sizeCodec() {
                return sizeCodec;
            }

            @Override
            public PaddingLengthGenerator padding() {
                return EmptyPaddingLengthGenerator.INSTANCE;
            }

            @Override
            public void decodePacket(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
                byte[] payloadBytes = new byte[in.readableBytes()];
                in.readBytes(payloadBytes);
                out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
            }
        };
    }

    static long now() {
        return Instant.now().getEpochSecond();
    }

    // session_subkey := blake3::derive_key(context: "shadowsocks 2022 session subkey", key_material: key + salt)
    static byte[] sessionSubkey(byte[] key, byte[] salt) {
        Blake3Digest digest = new Blake3Digest();
        Blake3Parameters parameters = Blake3Parameters.context("shadowsocks 2022 session subkey".getBytes());
        digest.init(parameters);
        byte[] in = new byte[key.length + salt.length];
        System.arraycopy(key, 0, in, 0, key.length);
        System.arraycopy(salt, 0, in, key.length, salt.length);
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }
}
