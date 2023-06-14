package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.Auth;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.SecurityType;

import java.util.function.Predicate;

import static com.urbanspork.common.codec.vmess.AEADChunkSizeCodec.AUTH_LEN;

public class AEADBodyCodec {

    private AEADBodyCodec() {}

    public static AEADBodyEncoder getBodyEncoder(SecurityType type, Session session) {
        AEADCipherCodec codec = getAEADCipherCodec(type);
        Predicate<Session> predicate = ServerSession.class::isInstance;
        if (SecurityType.CHACHA20_POLY1305 == type) {
            return new AEADBodyEncoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(getKey(session, predicate)), getIV(session, predicate)),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN.getBytes())), session.getRequestBodyIV()),
                new ShakeSizeParser(getIV(session, predicate))
            );
        }
        return new AEADBodyEncoder(
            newAEADAuthenticator(codec, getKey(session, predicate), getIV(session, predicate)),
            newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN.getBytes()), session.getRequestBodyIV()),
            new ShakeSizeParser(getIV(session, predicate))
        );
    }

    public static AEADBodyDecoder getBodyDecoder(SecurityType type, Session session) {
        AEADCipherCodec codec = getAEADCipherCodec(type);
        Predicate<Session> predicate = ClientSession.class::isInstance;
        if (SecurityType.CHACHA20_POLY1305 == type) {
            return new AEADBodyDecoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(getKey(session, predicate)), getIV(session, predicate)),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN.getBytes())), session.getRequestBodyIV()),
                new ShakeSizeParser(getIV(session, predicate)));
        }
        return new AEADBodyDecoder(
            newAEADAuthenticator(codec, getKey(session, predicate), getIV(session, predicate)),
            newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN.getBytes()), session.getRequestBodyIV()),
            new ShakeSizeParser(getIV(session, predicate)));
    }

    private static AEADCipherCodec getAEADCipherCodec(SecurityType type) {
        if (SecurityType.CHACHA20_POLY1305 == type) {
            return AEADCipherCodecs.CHACHA20_POLY1305.get();
        } else {
            return AEADCipherCodecs.AES_GCM.get();
        }
    }

    private static byte[] getKey(Session session, Predicate<Session> predicate) {
        return predicate.test(session) ? session.getResponseBodyKey() : session.getRequestBodyKey();
    }

    private static byte[] getIV(Session session, Predicate<Session> predicate) {
        return predicate.test(session) ? session.getResponseBodyIV() : session.getRequestBodyIV();
    }

    private static AEADAuthenticator newAEADAuthenticator(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADAuthenticator(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize(), true), BytesGenerator.generateEmptyBytes());
    }

    private static AEADChunkSizeCodec newAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADChunkSizeCodec(newAEADAuthenticator(codec, key, nonce));
    }
}
