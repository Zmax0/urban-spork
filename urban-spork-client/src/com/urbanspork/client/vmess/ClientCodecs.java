package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.*;
import com.urbanspork.common.codec.vmess.VMessAEADBodyDecoder;
import com.urbanspork.common.codec.vmess.VMessAEADBodyEncoder;
import com.urbanspork.common.codec.vmess.VMessAEADChunkSizeCodec;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.Auth;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import static com.urbanspork.common.codec.vmess.VMessAEADChunkSizeCodec.AUTH_LEN;

class ClientCodecs {

    private ClientCodecs() {}

    static ClientAEADCodec get(String uuid, Socks5CommandRequest request, SupportedCipher cipher) {
        ClientSession session = new ClientSession();
        return switch (cipher) {
            case aes_128_gcm, aes_256_gcm -> new AESClientCodec(uuid, request, session, cipher);
            case chacha20_poly1305 -> new Chacha20Poly1305ClientCodec(uuid, request, session, cipher);
        };
    }

    static class AESClientCodec extends ClientAEADCodec {

        AESClientCodec(String uuid, Socks5CommandRequest request, ClientSession session, SupportedCipher cipher) {
            super(uuid, request, session, cipher);
        }

        @Override
        public AEADPayloadEncoder newBodyEncoder() {
            return new VMessAEADBodyEncoder(
                newAEADAuthenticator(super.get(), session.getRequestBodyKey(), session.getRequestBodyIV()),
                newAEADChunkSizeCodec(super.get(), KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV())
            );
        }

        @Override
        public AEADPayloadDecoder newBodyDecoder() {
            return new VMessAEADBodyDecoder(
                newAEADAuthenticator(super.get(), session.getResponseBodyKey(), session.getResponseBodyIV()),
                newAEADChunkSizeCodec(super.get(), KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN), session.getRequestBodyIV())
            );
        }
    }

    static class Chacha20Poly1305ClientCodec extends ClientAEADCodec {

        Chacha20Poly1305ClientCodec(String uuid, Socks5CommandRequest request, ClientSession session, SupportedCipher cipher) {
            super(uuid, request, session, cipher);
        }

        @Override
        public AEADPayloadEncoder newBodyEncoder() {
            AEADCipherCodec codec = AEADCipherCodecs.CHACHA20_POLY1305.get();
            return new VMessAEADBodyEncoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(session.getRequestBodyKey()), session.getRequestBodyIV()),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV())
            );
        }

        @Override
        public AEADPayloadDecoder newBodyDecoder() {
            AEADCipherCodec codec = AEADCipherCodecs.CHACHA20_POLY1305.get();
            return new VMessAEADBodyDecoder(
                newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(session.getResponseBodyKey()), session.getResponseBodyIV()),
                newAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.getRequestBodyKey(), AUTH_LEN)), session.getRequestBodyIV())
            );
        }
    }

    private static AEADAuthenticator newAEADAuthenticator(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADAuthenticator(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize(), true), BytesGenerator.generateEmptyBytes());
    }

    private static VMessAEADChunkSizeCodec newAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new VMessAEADChunkSizeCodec(newAEADAuthenticator(codec, key, nonce));
    }

}
