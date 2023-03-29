package com.urbanspork.common.codec.aead;

import com.urbanspork.common.protocol.vmess.VMessProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

public class VMessProtocolTestCase {

    @Test
    public void testFnv1a32() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] fnv1a32 = VMessProtocol.fnv1a32(data);
        Assertions.assertEquals("8Y5xCw==", Base64.getEncoder().encodeToString(fnv1a32));
    }

    @Test
    public void testSha256() {
        byte[] data = "public static void bubbleSort(int[] arr) {int len = arr.length;for (int i = 0; i < len; i++) {for (int j = 1; j < len - i; j++) {if (arr[j - 1] > arr[j]) {int tmp = arr[j - 1];arr[j - 1] = arr[j];arr[j] = tmp;}}}}".getBytes();
        byte[] sha256 = VMessProtocol.sha256(data);
        Assertions.assertEquals("7VMrlAuhxXrdwgz0uqgJAg==", Base64.getEncoder().encodeToString(Arrays.copyOf(sha256, 16)));
        Assertions.assertEquals("nUh/KfOu+nXOZD37A8EB/A==", Base64.getEncoder().encodeToString(Arrays.copyOfRange(sha256, 16, 32)));
    }

}
