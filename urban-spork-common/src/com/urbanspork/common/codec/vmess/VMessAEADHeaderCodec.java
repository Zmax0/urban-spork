package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.List;

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;

public record VMessAEADHeaderCodec(AEADCipherCodec codec) implements VMess {

    public void sealVMessAEADHeader(byte[] key, byte[] header, ByteBuf out) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidCipherTextException {
        byte[] generatedAuthID = AuthID.createAuthID(key, VMess.timestamp(30));
        byte[] connectionNonce = Dice.randomBytes(8);
        byte[] aeadPayloadLengthSerializedByte = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(aeadPayloadLengthSerializedByte).setShort(0, header.length);
        byte[] payloadHeaderLengthAEADEncrypted = codec.encrypt(
                KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, generatedAuthID, connectionNonce),
                KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, generatedAuthID, connectionNonce),
                generatedAuthID,
                aeadPayloadLengthSerializedByte
        );
        byte[] payloadHeaderAEADEncrypted = codec.encrypt(
                KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, generatedAuthID, connectionNonce),
                KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, generatedAuthID, connectionNonce),
                generatedAuthID,
                header
        );
        out.writeBytes(generatedAuthID); // 16
        out.writeBytes(payloadHeaderLengthAEADEncrypted); // 2 + TAG_SIZE
        out.writeBytes(connectionNonce); // 8
        out.writeBytes(payloadHeaderAEADEncrypted); // payload + TAG_SIZE
    }

    public void openVMessAEADHeader(byte[] key, ByteBuf msg, List<Object> out) throws InvalidCipherTextException {
        if (msg.readableBytes() < 16 + 2 + TAG_SIZE + 8 + TAG_SIZE) {
            return;
        }
        msg.markReaderIndex();
        byte[] authid = new byte[16];
        byte[] payloadHeaderLengthAEADEncrypted = new byte[2 + TAG_SIZE];
        byte[] nonce = new byte[8];
        msg.readBytes(authid);
        msg.readBytes(payloadHeaderLengthAEADEncrypted);
        msg.readBytes(nonce);
        byte[] decryptedAEADHeaderLengthPayloadResult = codec.decrypt(
                KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, authid, nonce),
                KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, authid, nonce),
                authid,
                payloadHeaderLengthAEADEncrypted
        );
        int length = Unpooled.wrappedBuffer(decryptedAEADHeaderLengthPayloadResult).readShort();
        // 16 == AEAD Tag size
        if (msg.readableBytes() < length + TAG_SIZE) {
            msg.resetReaderIndex();
            return;
        }
        byte[] payloadHeaderAEADEncrypted = new byte[length + TAG_SIZE];
        msg.readBytes(payloadHeaderAEADEncrypted);
        byte[] decryptedAEADHeaderPayloadR = codec.decrypt(
                KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, authid, nonce),
                KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, authid, nonce),
                authid,
                payloadHeaderAEADEncrypted
        );
        out.add(Unpooled.wrappedBuffer(decryptedAEADHeaderPayloadR));
    }

}
