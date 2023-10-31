package com.urbanspork.common.codec.chunk;

import com.urbanspork.common.util.Dice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmptyChunkSizeParserTestCase {
    @Test
    void testEncode() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> EmptyChunkSizeParser.INSTANCE.encode(1));
    }

    @Test
    void testDecode() {
        byte[] bytes = Dice.rollBytes(0);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> EmptyChunkSizeParser.INSTANCE.decode(bytes));
    }
}
