package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.Optional;

public interface AEADHeaderCodec {

    void encodeRequest(RequestHeader header, Session session, ByteBuf out) throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException;

    Optional<DecodeResult> decodeRequest(ByteBuf in) throws InvalidCipherTextException;

    record DecodeResult(RequestHeader header, Session session) {}
}
