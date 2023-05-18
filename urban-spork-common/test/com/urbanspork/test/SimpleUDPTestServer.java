package com.urbanspork.test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class SimpleUDPTestServer {

    public static final int PORT = 16800;

    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.printf("UDP test server startup [%d]\n", socket.getLocalPort());
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                System.out.printf("Receive msg from [%s]: %s", address, str);
                byte[] bytes = String.format("Received your msg [%s] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, address);
                socket.send(msg);
                System.out.println(" => Callback");
                if ("close".equals(str)) {
                    break;
                }
            }
        }
    }
}
