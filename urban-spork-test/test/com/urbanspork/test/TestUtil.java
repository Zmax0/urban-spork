package com.urbanspork.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

    private TestUtil() {}

    public static int freePort() {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return port;
    }

    public static int[] freePorts(int size) {
        int[] ports = new int[size];
        List<ServerSocket> sockets = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                ServerSocket socket;
                try {
                    socket = new ServerSocket(0);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                ports[i] = socket.getLocalPort();
                sockets.add(socket);
            }
        } finally {
            for (ServerSocket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("Close socket error", e);
                }
            }
        }
        return ports;
    }

}