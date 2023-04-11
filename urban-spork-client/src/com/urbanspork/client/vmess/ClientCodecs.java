package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.Auth;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import static com.urbanspork.client.vmess.ClientAEADChunkSizeCodec.AUTH_LEN;

class ClientCodecs {

    static ClientAEADCodec get(String uuid, Socks5CommandRequest request, SupportedCipher cipher) {
        ClientSession session = new ClientSession();
        return switch (cipher) {
            case aes_128_gcm -> new AESClientCodec(uuid, request, session, cipher);
            case chacha20_poly1305 -> new Chacha20Poly1305ClientCodec(uuid, request, session, cipher);
            default -> throw new UnsupportedOperationException(cipher + " is not supported for vmess protocol");
        };
    }

    static class AESClientCodec extends ClientAEADCodec {

        AESClientCodec(String uuid, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
            super(uuid, address, session, cipher);
        }

        @Override
        public ClientBodyEncoder newClientBodyEncoder() {
            return new ClientBodyEncoder(
                    newAEADAuthenticator(get(), session.requestBodyKey, session.requestBodyIV),
                    newClientAEADChunkSizeCodec(get(), KDF.kdf16(session.requestBodyKey, AUTH_LEN), session.requestBodyIV)
            );
        }

        @Override
        public AEADPayloadDecoder newClientBodyDecoder() {
            return new AEADPayloadDecoder(
                    newAEADAuthenticator(get(), session.responseBodyKey, session.responseBodyIV),
                    newClientAEADChunkSizeCodec(get(), KDF.kdf16(session.requestBodyKey, AUTH_LEN), session.requestBodyIV)
            );
        }
    }

    static class Chacha20Poly1305ClientCodec extends ClientAEADCodec {

        Chacha20Poly1305ClientCodec(String uuid, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
            super(uuid, address, session, cipher);
        }

        @Override
        public ClientBodyEncoder newClientBodyEncoder() {
            AEADCipherCodec codec = AEADCipherCodecs.CHACHA20_POLY1305.get();
            return new ClientBodyEncoder(
                    newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(session.requestBodyKey), session.requestBodyIV),
                    newClientAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.requestBodyKey, AUTH_LEN)), session.requestBodyIV)
            );
        }

        @Override
        public AEADPayloadDecoder newClientBodyDecoder() {
            AEADCipherCodec codec = AEADCipherCodecs.CHACHA20_POLY1305.get();
            return new AEADPayloadDecoder(
                    newAEADAuthenticator(codec, Auth.generateChacha20Poly1305Key(session.responseBodyKey), session.responseBodyIV),
                    newClientAEADChunkSizeCodec(codec, Auth.generateChacha20Poly1305Key(KDF.kdf16(session.requestBodyKey, AUTH_LEN)), session.requestBodyIV)
            );
        }
    }

    private static AEADAuthenticator newAEADAuthenticator(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new AEADAuthenticator(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize(), true), BytesGenerator.generateEmptyBytes());
    }

    private static ClientAEADChunkSizeCodec newClientAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        return new ClientAEADChunkSizeCodec(newAEADAuthenticator(codec, key, nonce));
    }

}
