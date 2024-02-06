package com.urbanspork.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Common - Dice")
class DiceTestCase {
    @Test
    void testAllZeros() {
        Assertions.assertFalse(Dice.allZeros(new byte[]{0, 0, 1}));
        Assertions.assertTrue(Dice.allZeros(new byte[1024]));
    }
}
