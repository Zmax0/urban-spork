package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.List;

public interface VMessAEADHeaderCodec extends AEADCipherCodec, VMess {

    default void sealVMessAEADHeader(byte[] key, ByteBuf header, ByteBuf out) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidCipherTextException {
        byte[] generatedAuthID = AuthID.createAuthID(key, VMess.timestamp(30));
        byte[] connectionNonce = CipherCodec.randomBytes(8);
        byte[] payloadHeaderLengthAEADEncrypted;
        {
            byte[] aeadPayloadLengthSerializedByte = new byte[Short.BYTES];
            Unpooled.wrappedBuffer(aeadPayloadLengthSerializedByte).setShort(0, header.readableBytes());
            payloadHeaderLengthAEADEncrypted = encrypt(
                    KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, generatedAuthID, connectionNonce),
                    KDF.kdf(key, nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, generatedAuthID, connectionNonce),
                    generatedAuthID,
                    aeadPayloadLengthSerializedByte
            );
        }
        byte[] payloadHeaderAEADEncrypted;
        {
            byte[] data = new byte[header.readableBytes()];
            header.readBytes(data);
            payloadHeaderAEADEncrypted = encrypt(
                    KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, generatedAuthID, connectionNonce),
                    KDF.kdf(key, nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, generatedAuthID, connectionNonce),
                    generatedAuthID,
                    data
            );
        }
        out.writeBytes(generatedAuthID); // 16
        out.writeBytes(payloadHeaderLengthAEADEncrypted); // 2+16
        out.writeBytes(connectionNonce); // 8
        out.writeBytes(payloadHeaderAEADEncrypted); // payload
    }

    default void openVMessAEADHeader(byte[] key, ByteBuf header, List<Object> out) throws InvalidCipherTextException {
        if (header.readableBytes() < TAG_SIZE + 2 + TAG_SIZE + 8) {
            return;
        }
        header.markReaderIndex();
        byte[] authid = new byte[16];
        byte[] payloadHeaderLengthAEADEncrypted = new byte[2 + TAG_SIZE];
        byte[] nonce = new byte[8];
        header.readBytes(authid);
        header.readBytes(payloadHeaderLengthAEADEncrypted);
        header.readBytes(nonce);
        byte[] decryptedAEADHeaderLengthPayloadResult = decrypt(
                KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, authid, nonce),
                KDF.kdf(key, nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, authid, nonce),
                authid,
                payloadHeaderLengthAEADEncrypted
        );
        int length = Unpooled.wrappedBuffer(decryptedAEADHeaderLengthPayloadResult).readShort();
        // 16 == AEAD Tag size
        if (header.readableBytes() < length + TAG_SIZE) {
            header.resetReaderIndex();
            return;
        }
        byte[] decryptedAEADHeaderPayloadR;
        {
            byte[] payloadHeaderAEADEncrypted = new byte[length + TAG_SIZE];
            header.readBytes(payloadHeaderAEADEncrypted);
            decryptedAEADHeaderPayloadR = decrypt(
                    KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, authid, nonce),
                    KDF.kdf(key, nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, authid, nonce),
                    authid,
                    payloadHeaderAEADEncrypted
            );
        }
        out.add(Unpooled.wrappedBuffer(decryptedAEADHeaderPayloadR));
    }
}
