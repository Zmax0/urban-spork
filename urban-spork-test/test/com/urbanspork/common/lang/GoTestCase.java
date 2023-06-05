package com.urbanspork.common.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("Go")
class GoTestCase {
    @Test
    void testFnv1a32() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] fnv1a32 = Go.fnv1a32(data);
        Assertions.assertEquals("8Y5xCw==", Base64.getEncoder().encodeToString(fnv1a32));
    }
}
