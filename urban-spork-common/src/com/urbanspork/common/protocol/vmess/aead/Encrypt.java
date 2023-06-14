package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;

public class Encrypt {

    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY = "VMess Header AEAD Key_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV = "VMess Header AEAD Nonce_Length".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY = "VMess Header AEAD Key".getBytes();
    private static final byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV = "VMess Header AEAD Nonce".getBytes();

    private Encrypt() {}

    public static void sealVMessAEADHeader(byte[] key, byte[] header, ByteBuf out)
        throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidCipherTextException {
        byte[] generatedAuthID = AuthID.createAuthID(key, VMess.timestamp(30));
        byte[] connectionNonce = Dice.randomBytes(8);
        byte[] aeadPayloadLengthSerializedByte = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(aeadPayloadLengthSerializedByte).setShort(0, header.length);
        AEADCipherCodec cipher = AEADCipherCodecs.AES_GCM.get();
        int nonceSize = cipher.nonceSize();
        byte[] payloadHeaderLengthAEADEncrypted = cipher.encrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, generatedAuthID, connectionNonce),
            KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, generatedAuthID, connectionNonce),
            generatedAuthID,
            aeadPayloadLengthSerializedByte
        );
        byte[] payloadHeaderAEADEncrypted = cipher.encrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, generatedAuthID, connectionNonce),
            KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, generatedAuthID, connectionNonce),
            generatedAuthID,
            header
        );
        out.writeBytes(generatedAuthID); // 16
        out.writeBytes(payloadHeaderLengthAEADEncrypted); // 2 + TAG_SIZE
        out.writeBytes(connectionNonce); // 8
        out.writeBytes(payloadHeaderAEADEncrypted); // payload + TAG_SIZE
    }

    public static ByteBuf openVMessAEADHeader(byte[] key, ByteBuf in) throws InvalidCipherTextException {
        if (in.readableBytes() < 16 + 2 + TAG_SIZE + 8 + TAG_SIZE) {
            return Unpooled.EMPTY_BUFFER;
        }
        in.markReaderIndex();
        byte[] authid = new byte[16];
        byte[] payloadHeaderLengthAEADEncrypted = new byte[2 + TAG_SIZE];
        byte[] nonce = new byte[8];
        in.readBytes(authid);
        in.readBytes(payloadHeaderLengthAEADEncrypted);
        in.readBytes(nonce);
        AEADCipherCodec cipher = AEADCipherCodecs.AES_GCM.get();
        int nonceSize = cipher.nonceSize();
        byte[] decryptedAEADHeaderLengthPayloadResult = cipher.decrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY, authid, nonce),
            KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV, authid, nonce),
            authid,
            payloadHeaderLengthAEADEncrypted
        );
        int length = Unpooled.wrappedBuffer(decryptedAEADHeaderLengthPayloadResult).readShort();
        // 16 == AEAD Tag size
        if (in.readableBytes() < length + TAG_SIZE) {
            in.resetReaderIndex();
            return Unpooled.EMPTY_BUFFER;
        }
        byte[] payloadHeaderAEADEncrypted = new byte[length + TAG_SIZE];
        in.readBytes(payloadHeaderAEADEncrypted);
        byte[] decryptedAEADHeaderPayloadR = cipher.decrypt(
            KDF.kdf16(key, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY, authid, nonce),
            KDF.kdf(key, nonceSize, KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV, authid, nonce),
            authid,
            payloadHeaderAEADEncrypted
        );
        return Unpooled.wrappedBuffer(decryptedAEADHeaderPayloadR);
    }
}
