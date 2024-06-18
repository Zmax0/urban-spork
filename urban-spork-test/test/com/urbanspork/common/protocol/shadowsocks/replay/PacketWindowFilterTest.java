package com.urbanspork.common.protocol.shadowsocks.replay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PacketWindowFilterTest {
    private static final long REJECT_AFTER_MESSAGES = Long.MAX_VALUE - (1L << 13);
    private static final long LIMIT = PacketWindowFilter.WINDOW_SIZE + 1;

    private final PacketWindowFilter filter = new PacketWindowFilter();

    @Test
    void test() {
        Assertions.assertTrue(validatePacketId(0));
        Assertions.assertTrue(validatePacketId(1));
        Assertions.assertFalse(validatePacketId(1));
        Assertions.assertTrue(validatePacketId(9));
        Assertions.assertTrue(validatePacketId(8));
        Assertions.assertTrue(validatePacketId(7));
        Assertions.assertFalse(validatePacketId(7));
        Assertions.assertTrue(validatePacketId(LIMIT));
        Assertions.assertTrue(validatePacketId(LIMIT - 1));
        Assertions.assertFalse(validatePacketId(LIMIT - 1));
        Assertions.assertTrue(validatePacketId(LIMIT - 2));
        Assertions.assertTrue(validatePacketId(2));
        Assertions.assertFalse(validatePacketId(2));
        Assertions.assertTrue(validatePacketId(LIMIT + 16));
        Assertions.assertFalse(validatePacketId(3));
        Assertions.assertFalse(validatePacketId(LIMIT + 16));
        Assertions.assertTrue(validatePacketId(LIMIT * 4));
        Assertions.assertTrue(validatePacketId(LIMIT * 4 - (LIMIT - 1)));
        Assertions.assertFalse(validatePacketId(10));
        Assertions.assertFalse(validatePacketId(LIMIT * 4 - LIMIT));
        Assertions.assertFalse(validatePacketId(LIMIT * 4 - (LIMIT + 1)));
        Assertions.assertTrue(validatePacketId(LIMIT * 4 - (LIMIT - 2)));
        Assertions.assertFalse(validatePacketId(LIMIT * 4 + 1 - LIMIT));
        Assertions.assertFalse(validatePacketId(0));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES));
        Assertions.assertTrue(validatePacketId(REJECT_AFTER_MESSAGES - 1));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES - 1));
        Assertions.assertTrue(validatePacketId(REJECT_AFTER_MESSAGES - 2));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES + 1));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES + 2));
        Assertions.assertFalse(validatePacketId(REJECT_AFTER_MESSAGES - 2));
        Assertions.assertTrue(validatePacketId(REJECT_AFTER_MESSAGES - 3));
        Assertions.assertFalse(validatePacketId(0)); // 34
        // Bulk test 1
        filter.reset();
        for (long i = 1; i <= PacketWindowFilter.WINDOW_SIZE; i++) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        Assertions.assertTrue(validatePacketId(0));
        Assertions.assertFalse(validatePacketId(0));
        // Bulk test 2
        filter.reset();
        for (long i = 2; i <= PacketWindowFilter.WINDOW_SIZE + 1; i++) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        Assertions.assertTrue(validatePacketId(1));
        Assertions.assertFalse(validatePacketId(0));
        // Bulk test 3
        filter.reset();
        for (long i = PacketWindowFilter.WINDOW_SIZE + 1; i >= 1; i--) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        // Bulk test 4
        filter.reset();
        for (long i = PacketWindowFilter.WINDOW_SIZE + 2; i >= 2; i--) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        Assertions.assertFalse(validatePacketId(0));
        // Bulk test 5
        filter.reset();
        for (long i = PacketWindowFilter.WINDOW_SIZE; i >= 1; i--) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        Assertions.assertTrue(validatePacketId(PacketWindowFilter.WINDOW_SIZE + 1));
        Assertions.assertFalse(validatePacketId(0));
        // Bulk test 6
        filter.reset();
        for (long i = PacketWindowFilter.WINDOW_SIZE; i >= 1; i--) {
            Assertions.assertTrue(validatePacketId(i), "i=" + i);
        }
        Assertions.assertTrue(validatePacketId(0));
        Assertions.assertTrue(validatePacketId(PacketWindowFilter.WINDOW_SIZE + 1));
    }

    private boolean validatePacketId(long packetId) {
        return filter.validatePacketId(packetId, REJECT_AFTER_MESSAGES);
    }

}