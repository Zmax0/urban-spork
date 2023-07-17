package com.urbanspork.test.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class SimpleUDPSender {
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getGlobal();
        try (DatagramSocket socket = new DatagramSocket(0); BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String bindPortInfo = MessageFormat.format("UDP test client bind port {0,number,#}", socket.getLocalPort());
            logger.info(bindPortInfo);
            logger.info("Enter a valid port 0~65535 (quit to end)");
            for (; ; ) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                int port = Integer.parseInt(line);
                byte[] bytes = "Msg of SimpleUDPTestClient".getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", port));
                String sendPacketInfo = MessageFormat.format("Send packet {0}", packet);
                logger.info(sendPacketInfo);
                socket.send(packet);
            }
        }
    }
}
