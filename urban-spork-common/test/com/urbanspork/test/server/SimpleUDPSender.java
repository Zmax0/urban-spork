package com.urbanspork.test.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Date;

public class SimpleUDPSender {
    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(0);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.printf("%tc - UDP test client bind port %s\n", new Date(), socket.getLocalPort());
            System.out.println("Enter a valid port 0~65535 (quit to end)");
            for (; ; ) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                int port = Integer.parseInt(line);
                byte[] bytes = "Msg of SimpleUDPTestClient".getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", port));
                System.out.printf("%tc - Send packet %s\n", new Date(), packet);
                socket.send(packet);
            }
        }
    }
}
