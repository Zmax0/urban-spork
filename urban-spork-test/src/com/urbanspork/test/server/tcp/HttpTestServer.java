package com.urbanspork.test.server.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class HttpTestServer {
    public static final int PORT = 16802;

    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger("HttpTestServer");
        try (ServerSocket server = new ServerSocket(PORT); ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            logger.info("Listening on " + server.getLocalSocketAddress());
            while (!executor.isShutdown()) {
                Socket inbound = server.accept();
                executor.submit(() -> {
                    try {
                        InputStream read = inbound.getInputStream();
                        byte[] bytes = new byte[1024];
                        int len = read.read(bytes);
                        logger.info("↓ Receive msg ↓\n" + new String(bytes, 0, len));
                        OutputStream write = inbound.getOutputStream();
                        write.write("HTTP/1.1 200 OK\r\nServer: HttpTestServer\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n".getBytes());
                        write.write(("<h1>" + System.currentTimeMillis() + "</h1>\r\n").getBytes());
                        write.flush();
                        inbound.close();
                    } catch (IOException e) {
                        logger.severe("io exception: " + e.getMessage());
                    }
                });
            }
        }
    }
}
