package com.urbanspork.common.codec.aead;

import com.urbanspork.jni.AeadCipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

public enum CipherMethod {
    AES_128_GCM(16, 12, 16),
    AES_256_GCM(32, 12, 16),
    CHACHA8_POLY1305(32, 12, 16),
    CHACHA20_POLY1305(32, 12, 16),
    XCHACHA8_POLY1305(32, 24, 16),
    XCHACHA20_POLY1305(32, 24, 16),
    ;

    private final int keySize;
    private final int nonceSize;
    private final int tagSize;

    CipherMethod(int keySize, int nonceSize, int tagSize) {
        this.keySize = keySize;
        this.nonceSize = nonceSize;
        this.tagSize = tagSize;
    }

    public int keySize() {
        return keySize;
    }

    public int nonceSize() {
        return nonceSize;
    }

    public int tagSize() {
        return tagSize;
    }

    public CipherInstance init(byte[] key) {
        if (key.length != keySize) {
            String msg = String.format("%s is expecting a %d bytes key, but found %d bytes", this, keySize, key.length);
            throw new IllegalArgumentException(msg);
        }
        AeadCipher cipher = switch (this) {
            case AES_128_GCM -> com.urbanspork.jni.aes128gcm.Cipher.newInstance(key);
            case AES_256_GCM -> com.urbanspork.jni.aes256gcm.Cipher.newInstance(key);
            case CHACHA8_POLY1305 -> com.urbanspork.jni.chacha8poly1305.Cipher.newInstance(key);
            case CHACHA20_POLY1305 -> com.urbanspork.jni.chacha20poly1305.Cipher.newInstance(key);
            case XCHACHA8_POLY1305 -> com.urbanspork.jni.xchacha8poly1305.Cipher.newInstance(key);
            case XCHACHA20_POLY1305 -> com.urbanspork.jni.xchacha20poly1305.Cipher.newInstance(key);
        };
        return new JniCipherInstance(cipher, tagSize);
    }

    static ByteBuffer region(ByteBuf buf, int index, int length) {
        if (!buf.isDirect() || buf.nioBufferCount() != 1) {
            return null;
        }
        return buf.nioBuffer(index, length).slice();
    }

    private record JniCipherInstance(AeadCipher cipher, int tagSize) implements CipherInstance {
        @Override
        public void encrypt(byte[] nonce, byte[] aad, byte[] buffer) {
            cipher.encrypt(nonce, aad, buffer);
        }

        @Override
        public int decrypt(byte[] nonce, byte[] aad, byte[] buffer) {
            cipher.decrypt(nonce, aad, buffer);
            return buffer.length - tagSize;
        }

        @Override
        public void encrypt(byte[] nonce, byte[] aad, ByteBuf dst, int index, int length) {
            ByteBuffer view = region(dst, index, length);
            if (view != null) {
                cipher.encryptDirect(nonce, aad, view);
                return;
            }
            byte[] buffer = new byte[length];
            dst.getBytes(index, buffer, 0, length);
            cipher.encrypt(nonce, aad, buffer);
            dst.setBytes(index, buffer, 0, length);
        }

        @Override
        public ByteBuf decrypt(byte[] nonce, byte[] aad, ByteBuf src, int index, int length) {
            ByteBuffer view = region(src, index, length);
            if (view != null) {
                cipher.decryptDirect(nonce, aad, view);
                return src.retainedSlice(index, length - tagSize);
            }
            byte[] buffer = new byte[length];
            src.getBytes(index, buffer, 0, length);
            cipher.decrypt(nonce, aad, buffer);
            return Unpooled.wrappedBuffer(buffer, 0, length - tagSize);
        }

        @Override
        public void close() throws Exception {
            cipher.close();
        }
    }
}
