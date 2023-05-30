package com.urbanspork.test.server.udp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DelayTestServer {

    public static final int PORT = 16801;
    public static final int MAX_DELAYED_SECOND = 5;

    public static void main(String[] args) throws IOException {
        launch(PORT);
    }

    public static void launch(int port) throws IOException {
        Logger logger = Logger.getGlobal();
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        try (DatagramSocket socket = new DatagramSocket(port)) {
            String startupInfo = MessageFormat.format("UDP test server startup [{0,number,#}]", socket.getLocalPort());
            logger.info(startupInfo);
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                byte[] bytes = MessageFormat.format("Received your msg [{0}] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, address);
                int id = System.identityHashCode(msg);
                service.schedule(() -> {
                    try {
                        logger.info(MessageFormat.format("Callback [id: {0,number,#}]", id));
                        socket.send(msg);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, ThreadLocalRandom.current().nextInt(0, MAX_DELAYED_SECOND), TimeUnit.SECONDS);
                String receivedMsgInfo = MessageFormat.format("Received msg from [{0}]: {1} [id: {2,number,#}]", address, str, id);
                logger.info(receivedMsgInfo);
                if ("close".equals(str)) {
                    break;
                }
            }
        }
    }
}
