package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.codec.aead.CipherInstance;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Encrypt {
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY = "VMess Header AEAD Key_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV = "VMess Header AEAD Nonce_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY = "VMess Header AEAD Key".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV = "VMess Header AEAD Nonce".getBytes();

    private Encrypt() {}

    public static void sealVMessAEADHeader(byte[] key, byte[] header, ByteBuf out) throws Exception {
        byte[] generatedAuthID = AuthID.createAuthID(key, VMess.timestamp(30));
        byte[] connectionNonce = Dice.rollBytes(8);
        byte[] aeadPayloadLengthSerializedByte = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(aeadPayloadLengthSerializedByte).setShort(0, header.length);
        CipherMethod method = CipherMethod.AES_128_GCM;
        int nonceSize = method.nonceSize();
        out.writeBytes(generatedAuthID); // 16
        try (CipherInstance instance = method.init(KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, generatedAuthID, connectionNonce))) {
            byte[] payloadHeaderLengthAEADEncrypted = instance.encrypt(
                KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, generatedAuthID, connectionNonce),
                generatedAuthID,
                aeadPayloadLengthSerializedByte
            );
            out.writeBytes(payloadHeaderLengthAEADEncrypted); // 2 + TAG_SIZE
        }
        out.writeBytes(connectionNonce); // 8
        try (CipherInstance instance = method.init(KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, generatedAuthID, connectionNonce))) {
            byte[] payloadHeaderAEADEncrypted = instance.encrypt(
                KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, generatedAuthID, connectionNonce),
                generatedAuthID,
                header
            );
            out.writeBytes(payloadHeaderAEADEncrypted); // payload + TAG_SIZE
        }
    }

    public static ByteBuf openVMessAEADHeader(byte[] key, ByteBuf in) throws Exception {
        CipherMethod method = CipherMethod.AES_128_GCM;
        int tagSize = method.tagSize();
        if (in.readableBytes() < 16 + 2 + tagSize + 8 + tagSize) {
            return Unpooled.EMPTY_BUFFER;
        }
        in.markReaderIndex();
        byte[] authid = new byte[16];
        byte[] payloadHeaderLengthAEADEncrypted = new byte[2 + tagSize];
        byte[] nonce = new byte[8];
        in.readBytes(authid);
        in.readBytes(payloadHeaderLengthAEADEncrypted);
        in.readBytes(nonce);
        int nonceSize = method.nonceSize();
        byte[] decryptedAEADHeaderLengthPayloadResult;
        try (CipherInstance instance = method.init(KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, authid, nonce))) {
            decryptedAEADHeaderLengthPayloadResult = instance.decrypt(
                KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, authid, nonce),
                authid,
                payloadHeaderLengthAEADEncrypted
            );
        }
        int length = Unpooled.wrappedBuffer(decryptedAEADHeaderLengthPayloadResult).readShort();
        if (in.readableBytes() < length + tagSize) {
            in.resetReaderIndex();
            return Unpooled.EMPTY_BUFFER;
        }
        byte[] payloadHeaderAEADEncrypted = new byte[length + tagSize];
        in.readBytes(payloadHeaderAEADEncrypted);
        try (CipherInstance instance = method.init(KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, authid, nonce))) {
            byte[] decryptedAEADHeaderPayloadR = instance.decrypt(
                KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, authid, nonce),
                authid,
                payloadHeaderAEADEncrypted
            );
            return Unpooled.wrappedBuffer(decryptedAEADHeaderPayloadR);
        }
    }
}
