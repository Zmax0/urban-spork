package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public interface PayloadDecoder {

    int INIT_LENGTH = -1;

    int payloadLength();

    void updatePayloadLength(int payloadLength);

    int paddingLength();

    void updatePaddingLength(int paddingLength);

    Authenticator auth();

    ChunkSizeCodec sizeCodec();

    PaddingLengthGenerator padding();

    /**
     * decrypt payload for TCP
     *
     * @param in  [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     * @param out payload
     */
    default void decodePayload(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        PaddingLengthGenerator padding = padding();
        int payloadLength = payloadLength();
        int paddingLength = paddingLength();
        ChunkSizeCodec sizeCodec = sizeCodec();
        int sizeBytes = sizeCodec.sizeBytes();
        while (in.readableBytes() >= (payloadLength == INIT_LENGTH ? sizeBytes : payloadLength)) {
            if (paddingLength == INIT_LENGTH) {
                paddingLength = padding == null ? 0 : padding.nextPaddingLength();
            }
            if (payloadLength == INIT_LENGTH) {
                byte[] payloadSizeBytes = new byte[sizeBytes];
                in.readBytes(payloadSizeBytes);
                payloadLength = sizeCodec.decode(payloadSizeBytes);
            } else {
                byte[] payloadBytes = new byte[payloadLength - paddingLength];
                in.readBytes(payloadBytes);
                out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
                in.skipBytes(paddingLength);
                payloadLength = INIT_LENGTH;
                paddingLength = INIT_LENGTH;
            }
        }
        updatePayloadLength(payloadLength);
        updatePaddingLength(paddingLength);
    }

    /**
     * decrypt packet for UDP
     *
     * @param in  [salt][encrypted payload][tag]
     * @param out payload
     */
    default void decodePacket(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int paddingLength = 0;
        PaddingLengthGenerator padding = padding();
        if (padding != null) {
            paddingLength = padding.nextPaddingLength();
        }
        ChunkSizeCodec sizeCodec = sizeCodec();
        int sizeBytes = sizeCodec.sizeBytes();
        byte[] payloadSizeBytes = new byte[sizeBytes];
        in.readBytes(payloadSizeBytes);
        int packetLength = sizeCodec.decode(payloadSizeBytes);
        byte[] payloadBytes = new byte[packetLength - paddingLength];
        in.readBytes(payloadBytes);
        out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
        in.skipBytes(paddingLength);
    }
}
