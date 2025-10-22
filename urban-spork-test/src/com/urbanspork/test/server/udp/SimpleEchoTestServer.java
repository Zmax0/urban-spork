package com.urbanspork.test.server.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class SimpleEchoTestServer {

    public static final int PORT = 16800;

    static void main() throws IOException {
        launch(PORT, new CompletableFuture<>());
    }

    public static void launch(int port, CompletableFuture<DatagramSocket> future) throws IOException {
        Logger logger = Logger.getGlobal();
        try (DatagramSocket socket = new DatagramSocket(port)) {
            future.complete(socket);
            String startupInfo = MessageFormat.format("UDP test server startup [{0}]", socket.getLocalSocketAddress());
            logger.info(startupInfo);
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                String receiveMsgInfo = MessageFormat.format("Receive msg from [{0}]: {1}", address, str);
                logger.info(receiveMsgInfo);
                byte[] bytes = MessageFormat.format("Received your msg [{0}] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, address);
                socket.send(msg);
                logger.info("Callback");
                if ("close".equals(str)) {
                    break;
                }
            }
        }
    }
}
