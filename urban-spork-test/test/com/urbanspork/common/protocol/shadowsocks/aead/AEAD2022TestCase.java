package com.urbanspork.common.protocol.shadowsocks.aead;

import com.urbanspork.common.codec.CipherKind;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.params.Blake3Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("Shadowsocks - AEAD-2022")
class AEAD2022TestCase {

    @Test
    void testBlake3() {
        Blake3Digest digest = new Blake3Digest();
        Blake3Parameters parameters = Blake3Parameters.context("shadowsocks 2022 identity subkey".getBytes());
        digest.init(parameters);
        byte[] bytes = "Personal search-enabled assistant for programmers".getBytes();
        digest.update(bytes, 0, bytes.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        Assertions.assertEquals("nb91ZIjdNzFcPmGRS3Lg7m+muVqat549+RSDluyVN4c=", Base64.getEncoder().encodeToString(out));
    }

    @Test
    void testSessionSubkey() {
        byte[] key = Base64.getDecoder().decode("Lc3tTx0BY6ZJ/fCwOx3JvF0I/anhwJBO5p2+FA5Vce4=");
        byte[] salt = Base64.getDecoder().decode("3oFO0VyLyGI4nFN0M9P+62vPND/L6v8IingaPJWTbJA=");
        byte[] bytes = AEAD2022.TCP.sessionSubkey(key, salt);
        Assertions.assertEquals("EdNE+4U8dVnHT0+poAFDK2bdlwfrHT61sUNr9WYPh+E=", Base64.getEncoder().encodeToString(bytes));
    }

    @Test
    void testValidateTimestamp() {
        long timestamp = AEAD2022.newTimestamp() + 2 * AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF;
        Assertions.assertThrows(DecoderException.class, () -> AEAD2022.validateTimestamp(timestamp));
    }

    @Test
    void testGetPaddingLength() {
        Assertions.assertTrue(AEAD2022.getPaddingLength(Unpooled.EMPTY_BUFFER) > 0);
    }

    @Test
    void testGetNonceLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AEAD2022.UDP.getNonceLength(CipherKind.aes_128_gcm));
    }
}
