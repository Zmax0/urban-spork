package com.urbanspork.common.test;

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
                System.out.printf("Receive client msg %s\n", str);
                byte[] bytes = String.format("Received your msg [%s] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort()));
                socket.send(msg);
                if ("close".equals(str)) {
                    break;
                }
            }
        }
    }

}
