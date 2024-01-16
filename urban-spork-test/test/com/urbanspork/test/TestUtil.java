package com.urbanspork.test;

import com.urbanspork.common.protocol.network.Network;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtil {

    private TestUtil() {}

    public static int freePort() {
        return freePorts(1)[0];
    }

    public static int[] freePorts(int size) {
        int[] ports = new int[size];
        for (int i = 0; i < ports.length; i++) {
            int port;
            do {
                port = ThreadLocalRandom.current().nextInt(49152, 65535);
            } while (!Arrays.contains(ports, port) && isBindPort(port, Network.TCP) && isBindPort(port, Network.UDP));
            ports[i] = port;
        }
        return ports;
    }

    private static boolean isBindPort(int port, Network network) {
        if (Network.UDP == network) {
            try {
                DatagramSocket socket = new DatagramSocket(port);
                socket.close();
                return false;
            } catch (SocketException e) {
                return true;
            }
        } else {
            try {
                ServerSocket socket = new ServerSocket(port);
                socket.close();
                return false;
            } catch (IOException e) {
                return true;
            }
        }
    }


    public static <T> void testEqualsAndHashcode(T t1, T t2) {
        Set<T> set = new HashSet<>();
        set.add(t1);
        set.add(t2);
        Assertions.assertEquals(t1, set.iterator().next());
        Assertions.assertTrue(set.contains(t1));
        Assertions.assertTrue(set.contains(t2));
        Map<T, T> map = new HashMap<>();
        map.put(t1, t2);
        map.remove(t2);
        Assertions.assertNotEquals(t1, map.get(t2));
        Assertions.assertNotEquals(t1, new Object());
    }
}
