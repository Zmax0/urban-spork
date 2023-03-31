package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;


class ClientBodyEncoder implements AEADPayloadEncoder {

    private final AEADAuthenticator authenticator;
    private final ChunkSizeCodec chunkSizeEncoder;

    ClientBodyEncoder(AEADCipherCodec codec, byte[] key, byte[] nonce, byte[] chunkSizeKey, byte[] chunkSizeNonce) {
        chunkSizeEncoder = new ClientAEADChunkSizeCodec(codec, chunkSizeKey, chunkSizeNonce);
        authenticator = new AEADAuthenticator(codec, key, nonce);
    }

    @Override
    public ChunkSizeCodec chunkSizeEncoder() {
        return chunkSizeEncoder;
    }

    @Override
    public AEADAuthenticator payloadEncoder() {
        return authenticator;
    }

    @Override
    public int maxPayloadLength() {
        return 0xffff;
    }
}
