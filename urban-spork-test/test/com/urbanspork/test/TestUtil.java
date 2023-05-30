package com.urbanspork.test;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfigTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;
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
                    throw new UncheckedIOException(e);
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

    public static ClientConfig testConfig(int clientPort, int serverPort) {
        ClientConfig config = new ClientConfig();
        config.setPort(clientPort);
        config.setIndex(0);
        config.setServers(List.of(ServerConfigTestCase.testConfig(serverPort)));
        return config;
    }

}
