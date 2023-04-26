package com.urbanspork.common.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class SimpleUDPTestClient {

    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(0);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.printf("UDP test client bind port %s\n", socket.getLocalPort());
            System.out.println("Enter text (quit to end)");
            for (; ; ) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                byte[] bytes = line.getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", SimpleUDPTestServer.PORT));
                socket.send(packet);
                byte[] data = new byte[1024];
                packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                System.out.printf("Receive server msg: \"%s\"\n", new String(data, 0, packet.getLength()));
            }
        }
    }
}
