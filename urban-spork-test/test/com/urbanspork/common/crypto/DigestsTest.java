package com.urbanspork.common.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

class DigestsTest {

    @Test
    void testSha256() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] hash = Digests.sha256.hash(data);
        Assertions.assertEquals("7VMrlAuhxXrdwgz0uqgJAg==", Base64.getEncoder().encodeToString(Arrays.copyOf(hash, 16)));
        Assertions.assertEquals("nUh/KfOu+nXOZD37A8EB/A==", Base64.getEncoder().encodeToString(Arrays.copyOfRange(hash, 16, 32)));
    }

    @Test
    void testMd5() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] hash = Digests.md5.hash(data);
        Assertions.assertEquals("TaO3E8p5UCbqs7p6t4TM3A==", Base64.getEncoder().encodeToString(hash));
    }

    @Test
    void testBlake3() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] hash = Digests.blake3.hash(data);
        Assertions.assertEquals("mW+mJTxIruXj0ke+marzpHHuF8G48gQKmAcvc6k5pc4=", Base64.getEncoder().encodeToString(hash));
    }
}
