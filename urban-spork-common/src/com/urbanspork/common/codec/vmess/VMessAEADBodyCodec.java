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

import static com.urbanspork.common.codec.vmess.VMessAEADChunkSizeCodec.AUTH_LEN;

public class VMessAEADBodyCodec {

    public static VMessAEADBodyEncoder getBodyEncoder(SecurityType type, Session session) {
        AEADCipherCodec codec = getAEADCipherCodec(type);
        if (SecurityType.CHACHA20_POLY1305 == type) {
            return new VMessAEADBodyEncoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(getKey(session, VMessAEADBodyCodec::isServer)), getIV(session, VMessAEADBodyCodec::isServer)),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV())
            );
        }
        return new VMessAEADBodyEncoder(
            newAEADAuthenticator(codec, getKey(session, VMessAEADBodyCodec::isServer), getIV(session, VMessAEADBodyCodec::isServer)),
            newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV())
        );
    }

    public static VMessAEADBodyDecoder getBodyDecoder(SecurityType type, Session session) {
        AEADCipherCodec codec = getAEADCipherCodec(type);
        if (SecurityType.CHACHA20_POLY1305 == type) {
            return new VMessAEADBodyDecoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(getKey(session, VMessAEADBodyCodec::isClient)), getIV(session, VMessAEADBodyCodec::isClient)),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV())
            );
        }
        return new VMessAEADBodyDecoder(
            newAEADAuthenticator(codec, getKey(session, VMessAEADBodyCodec::isClient), getIV(session, VMessAEADBodyCodec::isClient)),
            newAEADChunkSizeCodec(codec, KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV())
        );
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

    private static boolean isServer(Session session) {
        return session instanceof ServerSession;
    }

    private static boolean isClient(Session session) {
        return session instanceof ClientSession;
    }

    private static AEADAuthenticator newAEADAuthenticator(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADAuthenticator(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize(), true), BytesGenerator.generateEmptyBytes());
    }

    private static VMessAEADChunkSizeCodec newAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new VMessAEADChunkSizeCodec(newAEADAuthenticator(codec, key, nonce));
    }

}
