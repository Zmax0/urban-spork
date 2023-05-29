package com.urbanspork.test.server.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DelayedUDPTestServer {

    public static final int PORT = 16801;

    public static void main(String[] args) throws IOException {
        launch(PORT);
    }

    public static void launch(int port) throws IOException {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.printf("%tc - UDP test server startup [%d]\n", new Date(), socket.getLocalPort());
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                byte[] bytes = String.format("Received your msg [%s] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, address);
                int id = System.identityHashCode(msg);
                service.schedule(() -> {
                    try {
                        System.out.printf("%tc - Callback [%d]\n", new Date(), id);
                        socket.send(msg);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 5, TimeUnit.SECONDS);
                System.out.printf("%tc - Received msg from [%s]: %s [%d]\n", new Date(), address, str, id);
                if ("close".equals(str)) {
                    break;
                }
            }
        }
    }
}
