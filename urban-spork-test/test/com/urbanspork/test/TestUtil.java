package com.urbanspork.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class TestUtil {

    private TestUtil() {}

    public static int[] freePorts(int size) {
        int[] ports = new int[size];
        List<ServerSocket> sockets = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                ServerSocket socket;
                try {
                    socket = new ServerSocket(0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ports[i] = socket.getLocalPort();
                sockets.add(socket);
            }
        } finally {
            for (ServerSocket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException ignore) {}
            }
        }
        return ports;
    }
}
