package com.urbanspork.test.interop;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;

record ReferencePeer(String image, String executable, String[] arguments, String[] versionArguments, Map<String, Object> config) {
    @SuppressWarnings("resource")
    GenericContainer<?> container(Path directory, int port) {
        return new GenericContainer<>(DockerImageName.parse(image))
            .withNetworkMode("host")
            .withCopyFileToContainer(MountableFile.forHostPath(directory.resolve("peer.json")), "/interop/config.json")
            .withCopyFileToContainer(MountableFile.forClasspathResource("localhost.crt"), "/interop/localhost.crt")
            .withCopyFileToContainer(MountableFile.forClasspathResource("localhost.key"), "/interop/localhost.key")
            .withCreateContainerCmdModifier(command -> command.withEntrypoint(executable))
            .withCommand(arguments)
            .withStartupAttempts(1)
            .waitingFor(new LoopbackWait(port).withStartupTimeout(Duration.ofSeconds(30)))
            .withLogConsumer(frame -> {
                try {
                    Files.writeString(directory.resolve("peer.log"), frame.getUtf8String(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    String version(GenericContainer<?> container) throws IOException, InterruptedException {
        String[] command = new String[versionArguments.length + 1];
        command[0] = executable;
        System.arraycopy(versionArguments, 0, command, 1, versionArguments.length);
        var result = container.execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IOException("Version command failed: " + result.getStdout() + result.getStderr());
        }
        return result.getStdout() + result.getStderr();
    }

    private static final class LoopbackWait extends AbstractWaitStrategy {
        private final int port;

        private LoopbackWait(int port) {
            this.port = port;
        }

        @Override
        protected void waitUntilReady() {
            Awaitility.await("reference peer to open port " + port)
                .pollInSameThread()
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(100))
                .atMost(startupTimeout)
                .failFast(() -> {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IllegalStateException("Interrupted while waiting for reference peer");
                    }
                    if (!waitStrategyTarget.isRunning()) {
                        throw new IllegalStateException("Reference peer exited before opening port " + port);
                    }
                })
                .ignoreExceptionsInstanceOf(IOException.class)
                .until(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                        return true;
                    }
                });
        }
    }
}
