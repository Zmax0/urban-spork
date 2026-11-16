package com.urbanspork.test.interop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.urbanspork.client.Client;
import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.PacketEncoding;
import com.urbanspork.server.Server;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.server.tcp.EchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(90)
abstract class InteropTestSupport {
    static final String HOST = "127.0.0.1";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Runtime runtime;
    private String commit;

    abstract Protocol protocol();

    abstract ReferencePeer peer(InteropCase scenario, ServerConfig remote, int socksPort);

    @BeforeAll
    void setup() throws Exception {
        Process git = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
        if (!git.waitFor(5, TimeUnit.SECONDS)) {
            git.destroyForcibly();
            throw new IOException("git rev-parse timed out");
        }
        commit = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        Assertions.assertEquals(0, git.exitValue(), commit);
        runtime = new Runtime();
    }

    @AfterAll
    void tearDown() {
        if (runtime != null) {
            runtime.close();
        }
    }

    final void verify(InteropCase scenario, boolean invalidCredentials) throws Exception {
        Path directory = Path.of("target", "interop", protocol().name(), scenario + (invalidCredentials ? "-invalid-credentials" : ""));
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("peer.log"), "");
        ServerConfig local = configuration(scenario);
        ServerConfig remote = configuration(scenario);
        remote.setPassword(local.getPassword());
        if (invalidCredentials) {
            remote.setPassword(TestDice.rollPassword(protocol(), scenario.cipher()));
        }
        int socksPort = freePort();
        if (scenario.role() == InteropCase.Role.CLIENT) {
            remote.setPort(freePort());
            local.setPort(remote.getPort());
        }
        FileAppender<ILoggingEvent> appender = captureLogs(directory);
        try {
            writeJson(directory.resolve("urban-spork.json"), local);
            if (scenario.role() == InteropCase.Role.SERVER) {
                try (Server.Instances server = Server.launch(runtime, List.of(local))) {
                    remote.setPort(server.servers().getFirst().tcp().localAddress().getPort());
                    runPeer(scenario, remote, local, socksPort, directory, invalidCredentials);
                }
            } else {
                runPeer(scenario, remote, local, socksPort, directory, invalidCredentials);
            }
            Files.writeString(directory.resolve("result.txt"), "PASS\n");
        } catch (Exception | AssertionError e) {
            Files.writeString(directory.resolve("result.txt"), "FAIL\n" + e + "\n");
            System.err.println("Interop failure: " + directory.toAbsolutePath());
            for (String name : List.of("metadata.json", "urban-spork.json", "peer.json", "peer-version.txt", "peer.log", "urban-spork.log")) {
                Path file = directory.resolve(name);
                if (Files.exists(file)) {
                    System.err.println(name + ":\n" + Files.readString(file));
                }
            }
            throw e;
        } finally {
            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender);
            appender.stop();
        }
    }

    private void runPeer(InteropCase scenario, ServerConfig remote, ServerConfig local, int socksPort, Path directory, boolean invalidCredentials) throws Exception {
        ReferencePeer peer = peer(scenario, remote, socksPort);
        writeJson(directory.resolve("peer.json"), peer.config());
        writeJson(directory.resolve("urban-spork.json"), local);
        writeJson(
            directory.resolve("metadata.json"), Map.of(
                "commit", commit, "scenario", scenario.toString(), "image", peer.image(),
                "invalidCredentials", invalidCredentials, "payloadSeed", "0x5eed + size + sequence"
            )
        );
        int peerPort = scenario.role() == InteropCase.Role.CLIENT ? remote.getPort() : socksPort;
        try (GenericContainer<?> container = peer.container(directory.toAbsolutePath(), peerPort)) {
            container.start();
            Files.writeString(directory.resolve("peer-version.txt"), peer.version(container));
            if (scenario.role() == InteropCase.Role.CLIENT) {
                ClientConfig config = new ClientConfig();
                config.setHost(HOST);
                config.setPort(0);
                config.setServers(List.of(local));
                writeJson(directory.resolve("urban-spork.json"), config);
                try (Client.Instance client = Client.launch(runtime, config).get(10, TimeUnit.SECONDS)) {
                    writeJson(directory.resolve("urban-spork.json"), config);
                    checkTraffic(scenario, client.tcp().localAddress(), invalidCredentials);
                }
            } else {
                checkTraffic(scenario, new InetSocketAddress(HOST, socksPort), invalidCredentials);
            }
        }
    }

    private void checkTraffic(InteropCase scenario, InetSocketAddress proxy, boolean invalidCredentials) throws Exception {
        if (scenario.traffic() == InteropCase.Traffic.TCP) {
            try (EchoTestServer target = EchoTestServer.launch(0)) {
                if (invalidCredentials) {
                    Assertions.assertThrows(IOException.class, () -> InteropTraffic.tcp(proxy, target.localAddress()));
                    Assertions.assertEquals(0, target.receivedBytes(), "Unauthenticated payload reached the target");
                } else {
                    InteropTraffic.tcp(proxy, target.localAddress());
                }
            }
        } else {
            try (SimpleEchoTestServer target = SimpleEchoTestServer.launch(0)) {
                // VMess interop covers 64/1024-byte datagrams; regular urban-spork regressions cover 4096 bytes.
                int[] sizes = protocol() == Protocol.vmess ? new int[]{64, 1024} : new int[]{64, 1024, 4096};
                InteropTraffic.udp(runtime.childGroup(), proxy, target.localAddress(), sizes);
            }
        }
    }

    private ServerConfig configuration(InteropCase scenario) {
        ServerConfig config = new ServerConfig();
        config.setHost(HOST);
        config.setProtocol(protocol());
        config.setCipher(scenario.cipher());
        config.setPassword(TestDice.rollPassword(protocol(), scenario.cipher()));
        config.setPacketEncoding(PacketEncoding.None);
        config.setTransport(new Transport[]{Transport.TCP, Transport.UDP});
        if (scenario.tls()) {
            config.setSsl(SslUtil.getSslSetting());
        }
        if (scenario.websocket()) {
            WebSocketSetting ws = new WebSocketSetting();
            ws.setPath("/interop");
            config.setWs(ws);
        }
        return config;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(HOST))) {
            return socket.getLocalPort();
        }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.writeString(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value));
    }

    private static FileAppender<ILoggingEvent> captureLogs(Path directory) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level [%thread] %logger - %msg%n");
        encoder.start();
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(context);
        appender.setName("interop");
        appender.setFile(directory.resolve("urban-spork.log").toString());
        appender.setAppend(false);
        appender.setEncoder(encoder);
        appender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
        return appender;
    }
}
