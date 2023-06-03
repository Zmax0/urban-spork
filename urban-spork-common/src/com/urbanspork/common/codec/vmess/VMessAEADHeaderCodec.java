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

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;

public record VMessAEADHeaderCodec(AEADCipherCodec codec) {

    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY = "VMess Header AEAD Key_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV = "VMess Header AEAD Nonce_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY = "VMess Header AEAD Key".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV = "VMess Header AEAD Nonce".getBytes();

    public void sealVMessAEADHeader(byte[] key, byte[] header, ByteBuf out)
        throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidCipherTextException {
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

    public void openVMessAEADHeader(byte[] key, ByteBuf in, ByteBuf out) throws InvalidCipherTextException {
        if (in.readableBytes() < 16 + 2 + TAG_SIZE + 8 + TAG_SIZE) {
            return;
        }
        in.markReaderIndex();
        byte[] authid = new byte[16];
        byte[] payloadHeaderLengthAEADEncrypted = new byte[2 + TAG_SIZE];
        byte[] nonce = new byte[8];
        in.readBytes(authid);
        in.readBytes(payloadHeaderLengthAEADEncrypted);
        in.readBytes(nonce);
        byte[] decryptedAEADHeaderLengthPayloadResult = codec.decrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, authid, nonce), // secretKey
            KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, authid, nonce), // nonce
            authid, // associatedText
            payloadHeaderLengthAEADEncrypted
        );
        int length = Unpooled.wrappedBuffer(decryptedAEADHeaderLengthPayloadResult).readShort();
        // 16 == AEAD Tag size
        if (in.readableBytes() < length + TAG_SIZE) {
            in.resetReaderIndex();
            return;
        }
        byte[] payloadHeaderAEADEncrypted = new byte[length + TAG_SIZE];
        in.readBytes(payloadHeaderAEADEncrypted);
        byte[] decryptedAEADHeaderPayloadR = codec.decrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, authid, nonce),
            KDF.kdf(key, codec.nonceSize(), KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, authid, nonce),
            authid,
            payloadHeaderAEADEncrypted
        );
        out.writeBytes(decryptedAEADHeaderPayloadR);
    }

}
