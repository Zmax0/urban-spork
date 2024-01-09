package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.vmess.AEADBodyCodec;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.Address;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.urbanspork.common.protocol.vmess.aead.Const.*;
import static com.urbanspork.common.protocol.vmess.aead.Encrypt.sealVMessAEADHeader;

public class ClientAEADCodec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ClientAEADCodec.class);
    private final RequestHeader header;
    private final Session session;
    private PayloadEncoder bodyEncoder;
    private PayloadDecoder bodyDecoder;

    public ClientAEADCodec(CipherKind cipher, Socks5CommandRequest address, String uuid) {
        this(cipher, RequestCommand.TCP, address, uuid);
    }

    ClientAEADCodec(CipherKind cipher, RequestCommand command, Socks5CommandRequest address, String uuid) {
        this(RequestHeader.defaultHeader(SecurityType.valueOf(cipher), command, address, uuid), new ClientSession());
    }

    ClientAEADCodec(RequestHeader header, Session session) {
        this.header = header;
        this.session = session;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (bodyEncoder == null) {
            ByteBuf buffer = out.duplicate();
            buffer.writeByte(header.version()); // version
            buffer.writeBytes(session.getRequestBodyIV()); // requestBodyIV
            buffer.writeBytes(session.getRequestBodyKey()); // requestBodyKey
            buffer.writeByte(session.getResponseHeader()); // responseHeader
            buffer.writeByte(RequestOption.toMask(header.option())); // option
            int paddingLen = ThreadLocalRandom.current().nextInt(16); // dice roll 16
            SecurityType security = header.security();
            buffer.writeByte((paddingLen << 4) | security.getValue() & 0xff);
            buffer.writeByte(0);
            buffer.writeByte(header.command().value());
            Address.writeAddressPort(buffer, header.address()); // address
            buffer.writeBytes(Dice.rollBytes(paddingLen)); // padding
            buffer.writeBytes(Go.fnv1a32(ByteBufUtil.getBytes(buffer, buffer.readerIndex(), buffer.writerIndex(), false)));
            byte[] headerBytes = new byte[buffer.readableBytes()];
            buffer.readBytes(headerBytes);
            sealVMessAEADHeader(header.id(), headerBytes, out);
            bodyEncoder = AEADBodyCodec.getBodyEncoder(header, session);
        }
        if (RequestCommand.UDP.equals(header.command())) {
            bodyEncoder.encodePacket(msg, out);
        } else {
            bodyEncoder.encodePayload(msg, out);
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bodyDecoder == null) {
            CipherMethod cipher = CipherMethods.AES_GCM.get();
            int tagSize = cipher.tagSize();
            int nonceSize = cipher.nonceSize();
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY.getBytes());
            byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_LEN_IV.getBytes());
            byte[] aeadEncryptedResponseHeaderLength = new byte[Short.BYTES + tagSize];
            in.markReaderIndex();
            in.readBytes(aeadEncryptedResponseHeaderLength);
            int decryptedResponseHeaderLength = Unpooled.wrappedBuffer(cipher.decrypt(aeadResponseHeaderLengthEncryptionKey,
                aeadResponseHeaderLengthEncryptionIV, aeadEncryptedResponseHeaderLength)).readShort();
            if (in.readableBytes() < decryptedResponseHeaderLength + tagSize) {
                logger.info("Unexpected readable bytes for decoding client header: expecting {} but actually {}", decryptedResponseHeaderLength + tagSize, in.readableBytes());
                in.resetReaderIndex();
                return;
            }
            byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY.getBytes());
            byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV.getBytes());
            byte[] encryptedResponseHeaderBytes = new byte[decryptedResponseHeaderLength + tagSize];
            in.readBytes(encryptedResponseHeaderBytes);
            byte[] decryptedResponseHeaderBytes = cipher.decrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, encryptedResponseHeaderBytes);
            byte responseHeader = decryptedResponseHeaderBytes[0];
            if (session.getResponseHeader() != responseHeader) { // v[1]
                throw new DecoderException(String.format("Unexpected response header: expecting %d but actually %d", session.getResponseHeader(), responseHeader));
            }
            bodyDecoder = AEADBodyCodec.getBodyDecoder(header, session);
        }
        if (RequestCommand.UDP.equals(header.command())) {
            bodyDecoder.decodePacket(in, out);
        } else {
            bodyDecoder.decodePayload(in, out);
        }
    }
}
