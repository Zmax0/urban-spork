package com.urbanspork.common.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public interface Dice {

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    static int randomPort() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = 0;
        for (; ; ) {
            int port = random.nextInt(49152, 65535);
            try (ServerSocket socket = new ServerSocket(port)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                if (count == 5) {
                    throw new IllegalStateException("No available port");
                } else {
                    count++;
                }
            }
        }
    }

}
