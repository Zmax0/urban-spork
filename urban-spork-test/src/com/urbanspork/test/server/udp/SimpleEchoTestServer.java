package com.urbanspork.test.server.udp;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleEchoTestServer implements Closeable {

    public static final int PORT = 16800;
    private static final Logger logger = Logger.getGlobal();

    private final DatagramSocket socket;
    private final Thread thread;

    private SimpleEchoTestServer(DatagramSocket socket, Thread thread) {
        this.socket = socket;
        this.thread = thread;
    }

    static void main() throws IOException, InterruptedException {
        try (SimpleEchoTestServer server = launch(PORT)) {
            server.awaitTermination();
        }
    }

    public static SimpleEchoTestServer launch(int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(port, InetAddress.getLoopbackAddress());
        Thread thread = new Thread(() -> receive(socket), "simple-echo-test-server");
        thread.setDaemon(true);
        thread.start();
        logger.info(MessageFormat.format("UDP test server startup [{0}]", socket.getLocalSocketAddress()));
        return new SimpleEchoTestServer(socket, thread);
    }

    private static void receive(DatagramSocket socket) {
        try {
            for (; ; ) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String str = new String(data, 0, packet.getLength());
                InetSocketAddress address = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());
                logger.info(MessageFormat.format("Receive msg from [{0}]: {1}", address, str));
                byte[] bytes = MessageFormat.format("Received your msg [{0}] ^_^", str).getBytes();
                socket.send(new DatagramPacket(bytes, bytes.length, address));
                logger.info("Callback");
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
    }
}
