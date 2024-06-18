package com.urbanspork.common.codec.vmess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

class ShakeSizeParserTest {
    @Test
    void testNextPaddingLength() {
        ShakeSizeParser parser = newParser();
        Assertions.assertEquals(41, parser.nextPaddingLength());
        Assertions.assertEquals(20, parser.nextPaddingLength());
        Assertions.assertEquals(42, parser.nextPaddingLength());
        Assertions.assertEquals(18, parser.nextPaddingLength());
    }

    @Test
    void testEncodeAndDecode() {
        ShakeSizeParser parser1 = newParser();
        int size = ThreadLocalRandom.current().nextInt(32768, 65535);
        byte[] bytes = parser1.encode(size);
        ShakeSizeParser parser2 = newParser();
        Assertions.assertEquals(size, parser2.decode(bytes));
    }

    private static ShakeSizeParser newParser() {
        return new ShakeSizeParser("public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes());
    }
}
