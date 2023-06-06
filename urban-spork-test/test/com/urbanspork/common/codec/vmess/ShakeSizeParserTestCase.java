package com.urbanspork.common.codec.vmess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ShakeSizeParserTestCase {
    @Test
    void testNextPaddingLength() {
        ShakeSizeParser parser = new ShakeSizeParser("public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes());
        Assertions.assertEquals(41, parser.nextPaddingLength());
        Assertions.assertEquals(20, parser.nextPaddingLength());
        Assertions.assertEquals(42, parser.nextPaddingLength());
        Assertions.assertEquals(18, parser.nextPaddingLength());
    }
}
