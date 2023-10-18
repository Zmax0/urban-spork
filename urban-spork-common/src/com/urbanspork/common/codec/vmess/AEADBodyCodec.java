package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.EmptyPaddingLengthGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherCodec;
import com.urbanspork.common.codec.aead.CipherCodecs;
import com.urbanspork.common.codec.chunk.AEADChunkSizeParser;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.codec.chunk.PlainChunkSizeParser;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.Auth;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;

import java.util.function.Predicate;

public class AEADBodyCodec {

    private static final byte[] AUTH_LEN = "auth_len".getBytes();

    private AEADBodyCodec() {}

    public static AEADBodyEncoder getBodyEncoder(RequestHeader header, Session session) {
        Tuple tuple = newTuple(header, session, ServerSession.class::isInstance);
        SecurityType security = header.security();
        CipherCodec codec = getAEADCipherCodec(security);
        ChunkSizeCodec sizeParser = tuple.sizeParser;
        if (SecurityType.CHACHA20_POLY1305 == security) {
            if (RequestOption.has(header.option(), RequestOption.AuthenticatedLength)) {
                sizeParser = newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV());
            }
            return new AEADBodyEncoder(newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(tuple.key), tuple.iv), sizeParser, tuple.padding);
        }
        if (RequestOption.has(header.option(), RequestOption.AuthenticatedLength)) {
            sizeParser = newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV());
        }
        return new AEADBodyEncoder(newAEADAuthenticator(codec, tuple.key, tuple.iv), sizeParser, tuple.padding);
    }

    public static AEADBodyDecoder getBodyDecoder(RequestHeader header, Session session) {
        Tuple tuple = newTuple(header, session, ClientSession.class::isInstance);
        SecurityType security = header.security();
        CipherCodec codec = getAEADCipherCodec(security);
        ChunkSizeCodec sizeParser = tuple.sizeParser;
        if (SecurityType.CHACHA20_POLY1305 == security) {
            if (RequestOption.has(header.option(), RequestOption.AuthenticatedLength)) {
                sizeParser = newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV());
            }
            return new AEADBodyDecoder(newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(tuple.key), tuple.iv), sizeParser, tuple.padding);
        }
        if (RequestOption.has(header.option(), RequestOption.AuthenticatedLength)) {
            sizeParser = newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV());
        }
        return new AEADBodyDecoder(newAEADAuthenticator(codec, tuple.key, tuple.iv), sizeParser, tuple.padding);
    }

    private static CipherCodec getAEADCipherCodec(SecurityType security) {
        if (SecurityType.CHACHA20_POLY1305 == security) {
            return CipherCodecs.CHACHA20_POLY1305.get();
        } else {
            return CipherCodecs.AES_GCM.get();
        }
    }

    private static byte[] getKey(Session session, Predicate<Session> predicate) {
        return predicate.test(session) ? session.getResponseBodyKey() : session.getRequestBodyKey();
    }

    private static byte[] getIV(Session session, Predicate<Session> predicate) {
        return predicate.test(session) ? session.getResponseBodyIV() : session.getRequestBodyIV();
    }

    private static Authenticator newAEADAuthenticator(CipherCodec codec, byte[] key, byte[] nonce) {
        return new Authenticator(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize()), BytesGenerator.generateEmptyBytes());
    }

    private static ChunkSizeCodec newAEADChunkSizeCodec(CipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADChunkSizeParser(newAEADAuthenticator(codec, key, nonce));
    }

    private static Tuple newTuple(RequestHeader header, Session session, Predicate<Session> predicate) {
        byte[] key = getKey(session, predicate);
        byte[] iv = getIV(session, predicate);
        ChunkSizeCodec sizeParser = new PlainChunkSizeParser();
        PaddingLengthGenerator padding = EmptyPaddingLengthGenerator.INSTANCE;
        if (RequestOption.has(header.option(), RequestOption.ChunkMasking)) {
            sizeParser = new ShakeSizeParser(iv);
        }
        if (RequestOption.has(header.option(), RequestOption.GlobalPadding) && sizeParser instanceof PaddingLengthGenerator generator) {
            padding = generator;
        }
        return new Tuple(key, iv, sizeParser, padding);
    }

    private record Tuple(byte[] key, byte[] iv, ChunkSizeCodec sizeParser, PaddingLengthGenerator padding) {}
}
