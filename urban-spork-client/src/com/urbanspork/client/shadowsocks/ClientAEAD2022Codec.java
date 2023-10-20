package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherCodec;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.shadowsocks.AEAD2022CipherCodec;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestHeader;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD2022;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientAEAD2022Codec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ClientAEAD2022Codec.class);
    private final RequestHeader header;
    private final AEAD2022CipherCodec aead2022CipherCodec;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    public ClientAEAD2022Codec(RequestHeader header, AEAD2022CipherCodec aead2022CipherCodec) {
        this.header = header;
        this.aead2022CipherCodec = aead2022CipherCodec;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (header.network() == Network.UDP) {
            byte[] salt = Dice.rollBytes(aead2022CipherCodec.saltSize());
            out.writeBytes(salt);
            AEAD2022.newPayloadEncoder(aead2022CipherCodec.cipherCodec(), aead2022CipherCodec.secret(), salt).encodePacket(msg, out);
        } else {
            if (payloadEncoder == null) {
                byte[] salt = Dice.rollBytes(aead2022CipherCodec.saltSize());
                if (logger.isTraceEnabled()) {
                    logger.trace("new request salt {}", Base64.getEncoder().encodeToString(salt));
                }
                out.writeBytes(salt);
                payloadEncoder = AEAD2022.newPayloadEncoder(aead2022CipherCodec.cipherCodec(), aead2022CipherCodec.secret(), salt);
                ByteBuf temp = Unpooled.buffer();
                Address.encode(header.request(), temp);
                int paddingLength;
                if (msg.isReadable()) {
                    paddingLength = 0;
                } else {
                    paddingLength = ThreadLocalRandom.current().nextInt(AEAD2022.MIN_PADDING_LENGTH, AEAD2022.MAX_PADDING_LENGTH) + 1;
                }
                temp.writeShort(paddingLength);
                temp.writeBytes(Dice.rollBytes(paddingLength));
                msg = Unpooled.wrappedBuffer(temp, msg);
                for (byte[] bytes : AEAD2022.newRequestHeader(msg)) {
                    out.writeBytes(payloadEncoder.auth().seal(bytes));
                }
            }
            payloadEncoder.encodePayload(msg, out);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (header.network() == Network.UDP) {
            byte[] salt = new byte[aead2022CipherCodec.saltSize()];
            in.readBytes(salt);
            AEAD2022.newPayloadDecoder(aead2022CipherCodec.cipherCodec(), aead2022CipherCodec.secret(), salt).decodePacket(in, out);
        } else {
            if (payloadDecoder == null) {
                initPayloadDecoder(in, out);
                if (payloadDecoder == null) {
                    return;
                }
            }
            payloadDecoder.decodePayload(in, out);
        }
    }

    private void initPayloadDecoder(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        in.markReaderIndex();
        int saltSize = aead2022CipherCodec.saltSize();
        CipherCodec cipherCodec = aead2022CipherCodec.cipherCodec();
        int tagSize = cipherCodec.tagSize();
        byte[] salt = new byte[saltSize];
        in.readBytes(salt);
        if (logger.isTraceEnabled()) {
            logger.trace("response salt {}", Base64.getEncoder().encodeToString(salt));
        }
        PayloadDecoder newPayloadDecoder = AEAD2022.newPayloadDecoder(cipherCodec, aead2022CipherCodec.secret(), salt);
        byte[] encryptedHeaderBytes = new byte[1 + 8 + saltSize + 2 + tagSize];
        in.readBytes(encryptedHeaderBytes);
        Authenticator auth = newPayloadDecoder.auth();
        ByteBuf headerBuf = Unpooled.wrappedBuffer(auth.open(encryptedHeaderBytes));
        byte streamType = headerBuf.readByte();
        if (StreamType.Response.getValue() != streamType) {
            String msg = String.format("invalid stream type, expecting %d, but found %d", StreamType.Response.getValue(), streamType);
            throw new DecoderException(msg);
        }
        long timestamp = headerBuf.readLong();
        long now = AEAD2022.now();
        long diff = timestamp - now;
        if (Math.abs(diff) > AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF) {
            String msg = String.format("invalid timestamp %d - now %d = %d", timestamp, now, diff);
            throw new DecoderException(msg);
        }
        if (logger.isTraceEnabled()) {
            byte[] requestSalt = new byte[saltSize];
            headerBuf.readBytes(requestSalt);
            logger.trace("request salt {}", Base64.getEncoder().encodeToString(requestSalt));
        } else {
            headerBuf.skipBytes(saltSize);
        }
        int length = headerBuf.readUnsignedShort();
        if (in.readableBytes() < length + tagSize) {
            in.resetReaderIndex();
            return;
        }
        byte[] encryptedPayloadBytes = new byte[length + tagSize];
        in.readBytes(encryptedPayloadBytes);
        out.add(Unpooled.wrappedBuffer(auth.open(encryptedPayloadBytes)));
        this.payloadDecoder = newPayloadDecoder;
    }
}
