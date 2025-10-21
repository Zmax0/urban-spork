package com.urbanspork.test.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class SimpleTCPSender {

    static void main() throws IOException {
        Logger logger = Logger.getGlobal();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            logger.info("Enter a valid port 0~65535 (quit to end)");
            String line = in.readLine();
            if ("quit".equalsIgnoreCase(line)) {
                return;
            }
            int port = line.isEmpty() ? 16802 : Integer.parseInt(line);
            try (Socket socket = new Socket("172.27.77.53", port);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String bindPortInfo = MessageFormat.format("TCP test client bind port {0,number,#}", socket.getLocalPort());
                logger.info(bindPortInfo);
                String send = "Msg of SimpleTCPSender";
                String sendInfo = MessageFormat.format("Send msg: {0}", send);
                writer.println(send);
                writer.flush();
                logger.info(sendInfo);
                String receive = reader.readLine();
                String receiveInfo = MessageFormat.format("Receive msg: {0}", receive);
                logger.info(receiveInfo);
            }
        }
    }
}
