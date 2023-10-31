package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public record PayloadDecoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding, Length length) {

    private static final int INIT_LENGTH = -1;

    public PayloadDecoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) {
        this(auth, sizeCodec, padding, new Length());
    }

    /**
     * decrypt payload for TCP
     *
     * @param in  [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     * @param out payload
     */
    public void decodePayload(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int sizeBytes = sizeCodec.sizeBytes();
        while (in.readableBytes() >= (length.payload == INIT_LENGTH ? sizeBytes : length.payload)) {
            if (length.padding == INIT_LENGTH) {
                length.padding = padding.nextPaddingLength();
            }
            if (length.payload == INIT_LENGTH) {
                byte[] payloadSizeBytes = new byte[sizeBytes];
                in.readBytes(payloadSizeBytes);
                length.payload = sizeCodec.decode(payloadSizeBytes);
            } else {
                byte[] payloadBytes = new byte[length.payload - length.padding];
                in.readBytes(payloadBytes);
                out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
                in.skipBytes(length.padding);
                length.payload = INIT_LENGTH;
                length.padding = INIT_LENGTH;
            }
        }
    }

    /**
     * decrypt packet for UDP
     *
     * @param in  [encrypted payload][tag]
     * @param out payload
     */
    public void decodePacket(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        out.add(decodePacket(in));
    }

    public ByteBuf decodePacket(ByteBuf in) throws InvalidCipherTextException {
        int paddingLength = padding.nextPaddingLength();
        int sizeBytes = sizeCodec.sizeBytes();
        int packetLength;
        if (sizeBytes > 0) {
            byte[] payloadSizeBytes = new byte[sizeBytes];
            in.readBytes(payloadSizeBytes);
            packetLength = sizeCodec.decode(payloadSizeBytes);
        } else {
            packetLength = in.readableBytes();
        }
        byte[] payloadBytes = new byte[packetLength - paddingLength];
        in.readBytes(payloadBytes);
        in.skipBytes(paddingLength);
        return Unpooled.wrappedBuffer(auth().open(payloadBytes));
    }

    static class Length {
        int payload = INIT_LENGTH;
        int padding = INIT_LENGTH;
    }
}
