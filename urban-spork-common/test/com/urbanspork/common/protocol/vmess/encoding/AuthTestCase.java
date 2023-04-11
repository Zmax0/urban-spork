package com.urbanspork.common.protocol.vmess.encoding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class AuthTestCase {

    @Test
    void testGenerateChacha20Poly1305Key() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] key = Auth.generateChacha20Poly1305Key(data);
        Assertions.assertEquals("TaO3E8p5UCbqs7p6t4TM3EBzK1qXHrgGSxmSVBtcjLk=", Base64.getEncoder().encodeToString(key));
    }
}
