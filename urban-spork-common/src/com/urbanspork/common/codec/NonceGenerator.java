package com.urbanspork.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

@SuppressWarnings("unused")
public interface NonceGenerator extends BytesGenerator {

    static NonceGenerator generateIncreasingNonce(byte[] nonce) {
        return () -> {
            for (int i = 0; i < nonce.length; i++) {
                nonce[i]++;
                if (nonce[i] != 0) {
                    break;
                }
            }
            return nonce;
        };
    }

    static NonceGenerator generateInitialAEADNonce(byte[] nonce) {
        return generateIncreasingNonce(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
    }

    static NonceGenerator generateStaticBytes(byte[] nonce) {
        return () -> nonce;
    }

    static NonceGenerator generateCountingNonce(byte[] nonce, int nonceSize) {
        return new NonceGenerator() {
            private short count;

            @Override
            public byte[] generate() {
                ByteBuf buf = Unpooled.wrappedBuffer(nonce);
                buf.setShort(0, count++);
                return Arrays.copyOf(nonce, nonceSize);
            }
        };
    }

}
