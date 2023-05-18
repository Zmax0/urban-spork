package com.urbanspork.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class SimpleUDPSender {
    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(0);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.printf("UDP test client bind port %s\n", socket.getLocalPort());
            System.out.println("Enter a valid port 0~65535 (quit to end)");
            for (; ; ) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                int port = Integer.parseInt(line);
                byte[] bytes = "Msg of SimpleUDPTestClient".getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", port));
                System.out.printf("Send packet %s\n", packet);
                socket.send(packet);
            }
        }
    }
}
