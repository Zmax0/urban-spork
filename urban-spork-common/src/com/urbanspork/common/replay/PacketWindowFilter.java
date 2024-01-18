package com.urbanspork.common.replay;

/**
 * Package replay implements an efficient anti-replay algorithm as specified in RFC 6479
 */
public class PacketWindowFilter {
    private static final int BITMAP_LENGTH = 1024 / Long.BYTES;
    private static final long REDUNDANT_BIT_SHIFTS = 6; // 1 << 6 == 64 bits
    private static final long REDUNDANT_BITS = 1L << REDUNDANT_BIT_SHIFTS;
    private static final long BLOCK_MASK = BITMAP_LENGTH - 1L;
    private static final long BIT_MASK = REDUNDANT_BITS - 1L;
    static final long WINDOW_SIZE = (BITMAP_LENGTH - 1L) * REDUNDANT_BITS;

    final long[] bitmap = new long[BITMAP_LENGTH];
    long lastPacketId;
    final long windowSize;

    public PacketWindowFilter() {
        this(0, WINDOW_SIZE);
    }

    public PacketWindowFilter(long lastPacketId, long windowSize) {
        this.windowSize = windowSize;
        this.lastPacketId = lastPacketId;
    }

    public boolean validatePacketId(long packetId, long limit) {
        if (packetId >= limit) {
            return false;
        }
        long index = packetId >> REDUNDANT_BIT_SHIFTS;
        if (packetId > lastPacketId) {
            long current = lastPacketId >> REDUNDANT_BIT_SHIFTS;
            long diff = index - current;
            if (diff > BITMAP_LENGTH) { // something unusual in this case
                diff = BITMAP_LENGTH;
            }
            for (long d = 1; d <= diff; d++) {
                long i = d + current;
                bitmap[(int) (i & BLOCK_MASK)] = 0;
            }
            lastPacketId = packetId;
        } else if (packetId + windowSize < lastPacketId) {
            // the packet is too old and out of the window
            return false;
        }
        index &= BLOCK_MASK;
        long bitLocation = packetId & BIT_MASK;
        if ((bitmap[(int) index] & (1L << bitLocation)) != 0) {
            return false; // this packet has already been received
        }
        bitmap[(int) index] |= (1L << bitLocation);
        return true;
    }

    public void reset() {
        lastPacketId = 0;
        bitmap[0] = 0;
    }
}