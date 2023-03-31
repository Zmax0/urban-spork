package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodecSupplier;
import com.urbanspork.common.protocol.vmess.aead.Auth;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import static com.urbanspork.client.vmess.ClientAEADChunkSizeCodec.AUTH_LEN;

class ClientCodecs {

    static ClientAEADCodec get(String uuid, Socks5CommandRequest request, SupportedCipher cipher) {
        ClientSession session = new ClientSession();
        return switch (cipher) {
            case aes_128_gcm -> new AESClientCodec(uuid, request, session, cipher);
            case chacha20_poly1305 -> new Chacha20Poly1305ClientCodec(uuid, request, session, cipher);
            default -> throw new UnsupportedOperationException();
        };
    }

    static class AESClientCodec extends ClientAEADCodec {

        AESClientCodec(String uuid, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
            super(uuid, address, session, cipher);
        }

        @Override
        public ClientBodyEncoder newClientBodyEncoder() {
            return new ClientBodyEncoder(
                    AEADCipherCodecSupplier.AES_GCM.get(),
                    session.requestBodyKey,
                    session.requestBodyIV,
                    KDF.kdf16(session.requestBodyKey, AUTH_LEN),
                    session.requestBodyIV
            );
        }

        @Override
        public ClientBodyDecoder newClientBodyDecoder() {
            return new ClientBodyDecoder(
                    AEADCipherCodecSupplier.AES_GCM.get(),
                    session.responseBodyKey,
                    session.responseBodyIV,
                    KDF.kdf16(session.requestBodyKey, AUTH_LEN),
                    session.requestBodyIV
            );
        }
    }

    static class Chacha20Poly1305ClientCodec extends ClientAEADCodec {

        Chacha20Poly1305ClientCodec(String uuid, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
            super(uuid, address, session, cipher);
        }

        @Override
        public ClientBodyEncoder newClientBodyEncoder() {
            return new ClientBodyEncoder(
                    AEADCipherCodecSupplier.CHACHA20_POLY1305.get(),
                    Auth.generateChacha20Poly1305Key(session.requestBodyKey),
                    session.requestBodyIV,
                    Auth.generateChacha20Poly1305Key(KDF.kdf16(session.requestBodyKey, AUTH_LEN)),
                    session.requestBodyIV
            );
        }

        @Override
        public ClientBodyDecoder newClientBodyDecoder() {
            return new ClientBodyDecoder(
                    AEADCipherCodecSupplier.CHACHA20_POLY1305.get(),
                    Auth.generateChacha20Poly1305Key(session.responseBodyKey),
                    session.responseBodyIV,
                    Auth.generateChacha20Poly1305Key(KDF.kdf16(session.requestBodyKey, AUTH_LEN)),
                    session.requestBodyIV
            );
        }
    }

}
