package com.urbanspork.server.shadowsocks;

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

public class ServerAEAD2022Codec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ServerAEAD2022Codec.class);
    private final RequestHeader header;
    private final AEAD2022CipherCodec aead2022CipherCodec;
    private final byte[] requestSalt;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    public ServerAEAD2022Codec(RequestHeader header, AEAD2022CipherCodec aead2022CipherCodec) {
        this.header = header;
        this.aead2022CipherCodec = aead2022CipherCodec;
        this.requestSalt = new byte[aead2022CipherCodec.saltSize()];
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
                    logger.trace("new response salt {}", Base64.getEncoder().encodeToString(salt));
                }
                out.writeBytes(salt);
                payloadEncoder = AEAD2022.newPayloadEncoder(aead2022CipherCodec.cipherCodec(), aead2022CipherCodec.secret(), salt);
                for (byte[] bytes : AEAD2022.newResponseHeader(requestSalt, msg)) {
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
        CipherCodec cipherCodec = aead2022CipherCodec.cipherCodec();
        int tagSize = cipherCodec.tagSize();
        in.readBytes(requestSalt);
        if (logger.isTraceEnabled()) {
            logger.trace("request salt {}", Base64.getEncoder().encodeToString(requestSalt));
        }
        PayloadDecoder newPayloadDecoder = AEAD2022.newPayloadDecoder(cipherCodec, aead2022CipherCodec.secret(), requestSalt);
        byte[] encryptedHeaderBytes = new byte[1 + 8 + 2 + tagSize];
        in.readBytes(encryptedHeaderBytes);
        Authenticator auth = newPayloadDecoder.auth();
        ByteBuf headerBuf = Unpooled.wrappedBuffer(auth.open(encryptedHeaderBytes));
        byte streamType = headerBuf.readByte();
        if (StreamType.Request.getValue() != streamType) {
            String msg = String.format("invalid stream type, expecting %d, but found %d", StreamType.Request.getValue(), streamType);
            throw new DecoderException(msg);
        }
        long timestamp = headerBuf.readLong();
        long now = AEAD2022.now();
        long diff = timestamp - now;
        if (Math.abs(diff) > AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF) {
            String msg = String.format("invalid timestamp %d - now %d = %d", timestamp, now, diff);
            throw new DecoderException(msg);
        }
        int length = headerBuf.readUnsignedShort();
        if (in.readableBytes() < length + tagSize) {
            in.resetReaderIndex();
            return;
        }
        byte[] encryptedPayloadBytes = new byte[length + tagSize];
        in.readBytes(encryptedPayloadBytes);
        ByteBuf first = Unpooled.wrappedBuffer(auth.open(encryptedPayloadBytes));
        Address.decode(first, out);
        int paddingLength = first.readUnsignedShort();
        first.skipBytes(paddingLength);
        out.add(first);
        this.payloadDecoder = newPayloadDecoder;
    }
}
