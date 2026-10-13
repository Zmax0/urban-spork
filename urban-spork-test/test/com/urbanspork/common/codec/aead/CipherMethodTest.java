package com.urbanspork.common.codec.aead;

import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CipherMethodTest {
    @Test
    void testUndirectRegion() {
        ByteBuf buffer1 = Unpooled.buffer();
        Assertions.assertNull(CipherMethod.region(buffer1, 0, 1));
        buffer1.release();
        ByteBuf buffer2 = Unpooled.directBuffer().writeBytes(new byte[]{1, 2, 3, 4});
        ByteBuf buffer3 = Unpooled.directBuffer().writeBytes(new byte[]{5, 6, 7, 8});
        ByteBuf buffer4 = Unpooled.wrappedBuffer(buffer2, buffer3);
        Assertions.assertNull(CipherMethod.region(buffer4, 0, 1));
        buffer4.release();
    }

    @Test
    void testIllegalKeySize() {
        CipherMethod method = CipherMethod.AES_128_GCM;
        byte[] key = Dice.rollBytes(method.keySize() + 1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> method.init(key));
    }
}
