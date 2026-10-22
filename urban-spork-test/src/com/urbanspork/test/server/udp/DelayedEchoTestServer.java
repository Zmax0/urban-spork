package com.urbanspork.test.server.udp;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DelayedEchoTestServer implements Closeable {

    public static final int PORT = 16801;
    public static final int MAX_DELAYED_SECOND = 2;
    private static final Logger logger = Logger.getGlobal();

    private final DatagramSocket socket;
    private final ScheduledExecutorService service;
    private final Thread thread;

    private DelayedEchoTestServer(DatagramSocket socket, ScheduledExecutorService service, Thread thread) {
        this.socket = socket;
        this.service = service;
        this.thread = thread;
    }

    static void main() throws IOException, InterruptedException {
        try (DelayedEchoTestServer server = launch(PORT)) {
            server.awaitTermination();
        }
    }

    public static DelayedEchoTestServer launch(int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(port, InetAddress.getLoopbackAddress());
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        Thread thread = new Thread(() -> receive(socket, service), "delayed-echo-test-server");
        thread.setDaemon(true);
        thread.start();
        logger.info(MessageFormat.format("UDP test server start [{0}]", socket.getLocalSocketAddress()));
        return new DelayedEchoTestServer(socket, service, thread);
    }

    private static void receive(DatagramSocket socket, ScheduledExecutorService service) {
        try {
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                byte[] bytes = MessageFormat.format("Received your msg [{0}] ^_^", str).getBytes();
                DatagramPacket msg = new DatagramPacket(bytes, bytes.length, address);
                int id = System.identityHashCode(msg);
                service.schedule(
                    () -> {
                        try {
                            logger.info(MessageFormat.format("Callback [id: {0,number,#}]", id));
                            socket.send(msg);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, ThreadLocalRandom.current().nextInt(1, MAX_DELAYED_SECOND), TimeUnit.SECONDS
                );
                logger.info(MessageFormat.format("Received msg from [{0}]: {1} [id: {2,number,#}]", address, str, id));
            }
        } catch (SocketException e) {
            logger.info("UDP test server closed");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "UDP test server io exception", e);
        }
    }

    public InetSocketAddress localAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    public void awaitTermination() throws InterruptedException {
        thread.join();
    }

    @Override
    public void close() {
        socket.close();
        service.shutdownNow();
    }
}
