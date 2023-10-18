package com.urbanspork.common.codec.shadowsocks;

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
import com.urbanspork.common.crypto.GeneralDigests;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestHeader;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.arraycopy;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/aead.html">AEAD ciphers</a>
 */
class AEADCipherCodec {
    /*
     * TCP per-session [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     * UDP per-packet [salt][encrypted payload][payload tag]
     */
    private final byte[] key;
    private final int saltSize;
    private final CipherCodec cipherCodec;
    private int payloadLength = PayloadDecoder.INIT_LENGTH;
    private boolean decodeAddress = false;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    AEADCipherCodec(String password, int saltSize, CipherCodec cipherCodec) {
        this.key = generateKey(password.getBytes(), saltSize);
        this.saltSize = saltSize;
        this.cipherCodec = cipherCodec;
    }

    public void encode(RequestHeader header, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (header.network() == Network.UDP) {
            ByteBuf in = Unpooled.buffer();
            Address.encode(header.request(), in);
            in.writeBytes(msg);
            byte[] salt = Dice.rollBytes(saltSize);
            out.writeBytes(salt);
            newPayloadEncoder(salt).encodePacket(in, out);
            in.release();
        } else {
            if (payloadEncoder == null) {
                byte[] salt = Dice.rollBytes(saltSize);
                out.writeBytes(salt);
                payloadEncoder = newPayloadEncoder(salt);
                if (StreamType.Request == header.streamType()) {
                    ByteBuf in = Unpooled.buffer();
                    Address.encode(header.request(), in);
                    in.writeBytes(msg);
                    msg = in;
                }
            }
            payloadEncoder.encodePayload(msg, out);
        }
    }

    public void decode(RequestHeader header, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (header.network() == Network.UDP) {
            byte[] salt = new byte[saltSize];
            in.readBytes(salt);
            List<Object> list = new ArrayList<>(1);
            newPayloadDecoder(salt).decodePacket(in, list);
            ByteBuf decoded = (ByteBuf) list.get(0);
            Address.decode(decoded, out);
            out.add(decoded.slice());
        } else {
            if (payloadDecoder == null && in.readableBytes() >= saltSize) {
                byte[] salt = new byte[saltSize];
                in.readBytes(salt);
                payloadDecoder = newPayloadDecoder(salt);
                decodeAddress = StreamType.Response == header.streamType();
            }
            if (payloadDecoder != null) {
                payloadDecoder.decodePayload(in, out);
                if (decodeAddress && !out.isEmpty()) {
                    out.add(0, Address.decode((ByteBuf) out.get(0)));
                    decodeAddress = false;
                }
            }
        }
    }

    // ensure key.length equals salt.length
    private byte[] generateKey(byte[] password, int size) {
        byte[] encoded = new byte[size];
        byte[] passwordDigest = GeneralDigests.md5.get(password);
        byte[] container = new byte[password.length + passwordDigest.length];
        arraycopy(passwordDigest, 0, encoded, 0, Math.min(size, passwordDigest.length));
        int index = passwordDigest.length;
        while (index < size) {
            arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
            arraycopy(password, 0, container, passwordDigest.length, password.length);
            passwordDigest = GeneralDigests.md5.get(container);
            arraycopy(passwordDigest, 0, encoded, index, Math.min(size - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }

    private PayloadEncoder newPayloadEncoder(byte[] salt) {
        Authenticator auth = new Authenticator(cipherCodec, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(),
            BytesGenerator.generateEmptyBytes());
        AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
        return new PayloadEncoder() {
            @Override
            public int payloadLimit() {
                return 0x3fff;
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

    private PayloadDecoder newPayloadDecoder(byte[] salt) {
        Authenticator auth = new Authenticator(cipherCodec, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(),
            BytesGenerator.generateEmptyBytes());
        AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
        return new PayloadDecoder() {
            @Override
            public int payloadLength() {
                return AEADCipherCodec.this.payloadLength;
            }

            @Override
            public void updatePayloadLength(int payloadLength) {
                AEADCipherCodec.this.payloadLength = payloadLength;
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

    private byte[] hkdfsha1(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, "ss-subkey".getBytes()));
        generator.generateBytes(out, 0, out.length);
        return out;
    }
}