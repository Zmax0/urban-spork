package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;

import java.util.List;

public record PayloadDecoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding, Length length) implements AutoCloseable {

    public PayloadDecoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) {
        this(auth, sizeCodec, padding, new Length());
    }

    /**
     * decrypt payload for TCP
     */
    public void decodePayload(ByteBuf in, List<Object> out) {
        while (in.isReadable()) {
            switch (length.state) {
                case Length -> {
                    int sizeBytes = sizeCodec.sizeBytes();
                    if (in.readableBytes() < sizeBytes) {
                        return;
                    }
                    byte[] payloadSizeBytes = new byte[sizeBytes];
                    in.readBytes(payloadSizeBytes);
                    length.payload = sizeCodec.decode(payloadSizeBytes);
                    length.state = State.Data;
                }
                case Data -> {
                    if (in.readableBytes() < length.payload) {
                        return;
                    }
                    out.add(auth.open(in, in.readerIndex(), length.payload - length.padding));
                    in.skipBytes(length.payload);
                    length.state = State.Padding;
                }
                default -> {
                    length.padding = padding.nextPaddingLength();
                    length.state = State.Length;
                }
            }
        }
    }

    /**
     * decrypt packet for UDP
     */
    public void decodePacket(ByteBuf in, List<Object> out) {
        out.add(decodePacket(in));
    }

    public ByteBuf decodePacket(ByteBuf in) {
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
        ByteBuf plaintext = auth.open(in, in.readerIndex(), packetLength - paddingLength);
        in.skipBytes(packetLength);
        return plaintext;
    }

    @Override
    public void close() throws Exception {
        auth.close();
    }

    private static class Length {
        State state = State.Padding;
        int payload;
        int padding;
    }

    private enum State {
        Padding,
        Length,
        Data,
    }
}
