package com.urbanspork.test.tool;

import com.urbanspork.common.protocol.https.proxy.Handshake;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

/**
 * VM options: {@code -DproxyHost=127.0.0.1 -DproxyPort=1089 -Dconnections=8 -Dpayload=1024m -Drate=16m}
 */
public class SpeedTestLocalServer {
    private static final Logger logger = LoggerFactory.getLogger(SpeedTestLocalServer.class);

    static void main() {
        String dstPort = System.getProperty("dstPort", String.valueOf(SpeedTestPeerServer.PORT));
        String host = System.getProperty("dstHost", InetAddress.getLoopbackAddress().getHostName());
        run(Options.fromSystem(new InetSocketAddress(host, Integer.parseInt(dstPort))));
    }

    static void run(Options options) {
        Metrics metrics = new Metrics(options);
        Tui tui = new Tui(options, metrics);
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        tui.start();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (ConnectionView view : metrics.connections) {
                futures.add(workers.submit(() -> drive(view, group, options, metrics)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            metrics.requestStop();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            metrics.workerFailure = e.getCause();
            metrics.event(-1, "worker failed: " + e.getCause());
        } finally {
            try {
                group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly();
            } finally {
                metrics.finish();
                tui.finish();
            }
        }
    }

    private static void drive(ConnectionView view, EventLoopGroup group, Options options, Metrics metrics) {
        Channel channel = null;
        try {
            if (metrics.stopping.isDone()) {
                return;
            }
            metrics.event(view.id, "CONNECT requested");
            long connectAt = metrics.now();
            var handshake = Handshake.start(group, options.proxy, options.peer);
            handshake.addListener(future -> {
                if (future.isSuccess() && (metrics.stopping.isDone() || view.terminal())) {
                    handshake.getNow().channel().close();
                }
            });
            long deadline = connectAt + TimeUnit.SECONDS.toNanos(10);
            while (!handshake.await(100, TimeUnit.MILLISECONDS)) {
                if (metrics.stopping.isDone()) {
                    return;
                }
                if (metrics.now() >= deadline) {
                    throw new TimeoutException("CONNECT timed out after 10s");
                }
            }
            channel = handshake.get().channel();
            metrics.track(view.id, channel);
            synchronized (view) {
                view.connectNanos = metrics.now() - connectAt;
                view.warmupAt = metrics.now();
            }
            metrics.transition(view, State.WARMUP, "configured relay warmup; readiness not measured");
            metrics.event(view.id, "CONNECT accepted; warmup " + options.relayWarmupMillis + "ms");
            if (options.relayWarmupMillis > 0) {
                try {
                    metrics.stopping.get(options.relayWarmupMillis, TimeUnit.MILLISECONDS);
                    return;
                } catch (TimeoutException expected) {
                    // Configured warmup elapsed without an operator stop.
                }
            }
            if (metrics.stopping.isDone()) {
                return;
            }
            if (!channel.isActive()) {
                throw new IOException("channel closed during configured warmup");
            }
            Channel connected = channel;
            DownloadHandler handler = new DownloadHandler(view, options, metrics);
            connected.eventLoop().submit(() -> {
                if (!view.terminal()) {
                    connected.pipeline().addLast(new HttpResponseDecoder(), handler);
                    synchronized (view) {
                        view.warmupNanos = metrics.now() - view.warmupAt;
                        view.requestAt = metrics.now();
                    }
                    metrics.transition(view, State.WAIT, "waiting for first HTTP body");
                    handler.armFirstByteTimeout(connected);
                    connected.writeAndFlush(Unpooled.wrappedBuffer(request(options))).addListener(future -> {
                        if (!future.isSuccess()) {
                            handler.fail(future.cause(), connected);
                        } else if (!view.terminal()) {
                            metrics.event(view.id, "HTTP GET sent; streaming decoder installed");
                        }
                    });
                } else {
                    handler.done.complete(null);
                }
            }).sync();
            handler.done.get();
        } catch (InterruptedException e) {
            metrics.requestStop();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            metrics.fail(view, e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e);
        } finally {
            if (channel != null) {
                Channel exiting = channel;
                exiting.eventLoop().submit(() -> {
                    exiting.config().setAutoRead(false);
                    view.reading(false, metrics.now(), 0);
                    exiting.close();
                }).syncUninterruptibly();
                exiting.closeFuture().syncUninterruptibly();
                metrics.channels.remove(view.id, channel);
            }
        }
    }

    private static byte[] request(Options options) {
        return ("GET /download/" + options.payload + " HTTP/1.1\r\n"
            + "Host: " + options.peer.getHostString() + ":" + options.peer.getPort() + "\r\n"
            + "Accept: */*\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    static final class DownloadHandler extends SimpleChannelInboundHandler<HttpObject> {
        final CompletableFuture<Void> done = new CompletableFuture<>();
        private final ConnectionView view;
        private final Options options;
        private final Metrics metrics;
        private ScheduledFuture<?> timeout;
        private ScheduledFuture<?> resume;
        private boolean accepted;

        DownloadHandler(ConnectionView view, Options options, Metrics metrics) {
            this.view = view;
            this.options = options;
            this.metrics = metrics;
        }

        void armFirstByteTimeout(Channel channel) {
            timeout = channel.eventLoop().schedule(
                () -> fail(new TimeoutException("no HTTP body within " + options.firstByteTimeoutMillis + "ms"), channel),
                options.firstByteTimeoutMillis, TimeUnit.MILLISECONDS
            );
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
            Channel channel = ctx.channel();
            if (view.terminal()) {
                return;
            }
            if (!message.decoderResult().isSuccess()) {
                fail(new IOException("invalid HTTP response", message.decoderResult().cause()), channel);
                return;
            }
            if (message instanceof HttpResponse response) {
                if (accepted || response.status().code() != 200) {
                    fail(new IOException("expected HTTP 200, received " + response.status()), channel);
                    return;
                }
                long length = HttpUtil.getContentLength(response, -1);
                if (length >= 0 && length != options.payload) {
                    fail(new IOException("Content-Length " + length + " differs from payload " + options.payload), channel);
                    return;
                }
                accepted = true;
                metrics.event(view.id, "HTTP 200 accepted");
            }
            if (message instanceof HttpContent content) {
                if (!accepted) {
                    fail(new IOException("HTTP body arrived before response headers"), channel);
                    return;
                }
                int size = content.content().readableBytes();
                if (size > 0) {
                    long now = metrics.now();
                    boolean first;
                    synchronized (view) {
                        if (view.state.terminal()) {
                            return;
                        }
                        first = view.firstBodyAt < 0;
                        if (first) {
                            view.firstBodyAt = now;
                            view.rate.reset(now);
                        }
                        view.lastBodyAt = now;
                        view.bytes += size;
                        view.rate.sample(now, view.bytes);
                    }
                    if (view.bytes > options.payload) {
                        fail(new IOException("HTTP body exceeds requested payload"), channel);
                        return;
                    }
                    if (first) {
                        cancel(timeout);
                        metrics.transition(view, State.READ, "receiving HTTP body");
                        metrics.event(view.id, "first HTTP body received");
                        if (options.mode == Mode.stalled) {
                            pause(channel, State.HOLD, now + TimeUnit.MILLISECONDS.toNanos(options.idleMillis));
                            metrics.event(view.id, "HOLD started; local autoRead off");
                            timeout = channel.eventLoop().schedule(
                                () -> {
                                    if (!view.terminal()) {
                                        metrics.transition(view, State.OBSERVED, "configured hold completed; upstream state not measured");
                                    }
                                    finish(channel);
                                }, options.idleMillis, TimeUnit.MILLISECONDS
                            );
                        } else {
                            timeout = channel.eventLoop().schedule(
                                () -> fail(new TimeoutException("download did not finish within " + options.completionTimeoutMillis + "ms"), channel),
                                options.completionTimeoutMillis, TimeUnit.MILLISECONDS
                            );
                        }
                    }
                    if (options.mode == Mode.throttled && options.rate > 0) {
                        throttle(channel, now);
                    }
                }
                if (message instanceof LastHttpContent && options.mode != Mode.stalled) {
                    if (view.bytes == options.payload) {
                        metrics.transition(view, State.DONE, "complete HTTP body received");
                        finish(channel);
                    } else {
                        fail(new IOException("HTTP body ended after " + view.bytes + " of " + options.payload + " bytes"), channel);
                    }
                }
            }
        }

        private void throttle(Channel channel, long now) {
            long deadline = view.firstBodyAt + (long) (view.bytes * 1_000_000_000.0 / options.rate);
            if (deadline <= now || view.terminal()) {
                return;
            }
            pause(channel, State.PAUSE, deadline);
            cancel(resume);
            resume = channel.eventLoop().schedule(
                () -> {
                    resume = null;
                    if (view.terminal()) {
                        return;
                    }
                    if (channel.isActive()) {
                        channel.config().setAutoRead(true);
                        view.reading(true, metrics.now(), 0);
                        metrics.transition(view, State.READ, "receiving HTTP body");
                    }
                }, deadline - now, TimeUnit.NANOSECONDS
            );
        }

        private void pause(Channel channel, State state, long deadline) {
            channel.config().setAutoRead(false);
            view.reading(false, metrics.now(), deadline);
            metrics.transition(view, state, state == State.HOLD ? "intentional hold (expected)" : "rate-controlled pause (expected)");
        }

        void fail(Throwable cause, Channel channel) {
            metrics.fail(view, cause);
            finish(channel);
        }

        private void finish(Channel channel) {
            cancel(timeout);
            cancel(resume);
            resume = null;
            channel.config().setAutoRead(false);
            view.reading(false, metrics.now(), 0);
            done.complete(null);
            channel.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!view.terminal()) {
                metrics.fail(view, new IOException("channel closed after " + view.bytes + " of " + options.payload + " body bytes"));
            }
            ctx.channel().config().setAutoRead(false);
            view.reading(false, metrics.now(), 0);
            cancel(timeout);
            cancel(resume);
            done.complete(null);
            ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            fail(cause, ctx.channel());
        }

        private static void cancel(ScheduledFuture<?> future) {
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    enum Mode {normal, throttled, stalled}

    enum State {
        CONNECT, WARMUP, WAIT, READ, PAUSE, HOLD, DONE, FAIL, STOP, OBSERVED;

        boolean terminal() {
            return this == DONE || this == FAIL || this == STOP || this == OBSERVED;
        }
    }

    static final class Metrics {
        final Options options;
        final List<ConnectionView> connections;
        final Map<Integer, Channel> channels = new ConcurrentHashMap<>();
        final CompletableFuture<Void> stopping = new CompletableFuture<>();
        private final ArrayDeque<Event> events = new ArrayDeque<>();
        private final LongSupplier clock;
        private final long startedAt;
        volatile long finishedAt = -1;
        volatile Throwable workerFailure;

        Metrics(Options options) {
            this(options, System::nanoTime);
        }

        Metrics(Options options, LongSupplier clock) {
            this.options = options;
            this.clock = clock;
            startedAt = clock.getAsLong();
            List<ConnectionView> views = new ArrayList<>();
            for (int id = 0; id < options.connections; id++) {
                views.add(new ConnectionView(id));
            }
            connections = List.copyOf(views);
        }

        long now() {
            return clock.getAsLong() - startedAt;
        }

        void track(int id, Channel channel) {
            channels.put(id, channel);
            if (stopping.isDone()) {
                channel.close();
            }
        }

        void requestStop() {
            if (stopping.complete(null)) {
                event(-1, "operator stop requested");
                for (ConnectionView view : connections) {
                    transition(view, State.STOP, "stopped by operator");
                }
                channels.values().forEach(Channel::close);
            }
        }

        void transition(ConnectionView view, State state, String detail) {
            if (view.state.terminal()) {
                return;
            }
            view.state = state;
            view.detail = detail;
            if (state.terminal()) {
                view.endedAt = now();
                view.endPause(now());
                event(view.id, state + ": " + detail);
            }
        }

        void fail(ConnectionView view, Throwable cause) {
            if (view.state.terminal()) {
                return;
            }
            if (stopping.isDone()) {
                transition(view, State.STOP, "stopped by operator");
                return;
            }
            view.failureStage = view.state;
            view.error = cause.getMessage() == null ? cause.toString() : cause.getMessage();
            transition(view, State.FAIL, view.error);
        }

        synchronized void event(int id, String text) {
            events.addLast(new Event(now(), id, text));
            while (events.size() > 100) {
                events.removeFirst();
            }
        }

        synchronized List<Event> events() {
            return List.copyOf(events);
        }

        List<ConnectionSnapshot> snapshot(long now) {
            return connections.stream().map(view -> view.snapshot(now)).toList();
        }

        String result(List<ConnectionSnapshot> views) {
            if (workerFailure != null || views.stream().anyMatch(view -> view.state == State.FAIL)) {
                return "FAILED";
            }
            if (stopping.isDone() || views.stream().anyMatch(view -> view.state == State.STOP)) {
                return "STOPPED";
            }
            State expected = options.mode == Mode.stalled ? State.OBSERVED : State.DONE;
            return !views.isEmpty() && views.stream().allMatch(view -> view.state == expected) ? "PASSED" : "FAILED";
        }

        void finish() {
            finishedAt = now();
        }
    }

    static final class ConnectionView {
        final int id;
        State state = State.CONNECT;
        State failureStage;
        String detail = "connecting to proxy";
        String error = "";
        volatile long bytes;
        volatile long firstBodyAt = -1;
        long lastBodyAt = -1;
        long endedAt = -1;
        long connectNanos = -1;
        long warmupAt = -1;
        long warmupNanos = -1;
        long requestAt = -1;
        boolean autoRead = true;
        long pauseSince = -1;
        long pausedNanos;
        long pauseCount;
        long resumeAt;
        final RateWindow rate = new RateWindow();

        ConnectionView(int id) {
            this.id = id;
        }

        synchronized boolean terminal() {
            return state.terminal();
        }

        synchronized void reading(boolean enabled, long now, long deadline) {
            if (!enabled && autoRead && !state.terminal()) {
                pauseSince = now;
                pauseCount++;
            } else if (enabled) {
                endPause(now);
            }
            autoRead = enabled;
            resumeAt = deadline;
        }

        void endPause(long now) {
            if (pauseSince >= 0) {
                pausedNanos += Math.max(0, now - pauseSince);
                pauseSince = -1;
            }
            resumeAt = 0;
        }

        synchronized ConnectionSnapshot snapshot(long now) {
            long paused = pausedNanos + (pauseSince < 0 ? 0 : Math.max(0, now - pauseSince));
            return new ConnectionSnapshot(
                id, state, bytes, rate.sample(now, bytes), autoRead, firstBodyAt, lastBodyAt, endedAt,
                connectNanos, warmupNanos, requestAt, pauseCount, paused, Math.max(0, resumeAt - now), detail, failureStage, error
            );
        }
    }

    record ConnectionSnapshot(int id, State state, long bytes, double rate, boolean autoRead, long firstBodyAt, long lastBodyAt, long endedAt,
                              long connectNanos, long warmupNanos, long requestAt, long pauseCount, long pausedNanos, long resumeNanos,
                              String detail, State failureStage, String error) {
        int priority() {
            if (state == State.FAIL) {
                return 0;
            }
            if (state == State.WAIT) {
                return 1;
            }
            return state.terminal() ? 3 : 2;
        }
    }

    static final class RateWindow {
        private final ArrayDeque<Sample> samples = new ArrayDeque<>(List.of(new Sample(0, 0)));

        void reset(long now) {
            samples.clear();
            samples.add(new Sample(now, 0));
        }

        double sample(long now, long bytes) {
            // Keep one sample per 100ms bucket; high throughput must not create a sample per buffer.
            if (samples.size() > 1 && samples.getLast().at / 100_000_000 == now / 100_000_000) {
                samples.removeLast();
            }
            samples.addLast(new Sample(now, bytes));
            long boundary = now - TimeUnit.SECONDS.toNanos(1);
            while (samples.size() > 2) {
                var iterator = samples.iterator();
                iterator.next();
                if (iterator.next().at > boundary) {
                    break;
                }
                samples.removeFirst();
            }
            Sample first = samples.getFirst();
            long start = first.at;
            double base = first.bytes;
            if (start < boundary && samples.size() >= 2) {
                var iterator = samples.iterator();
                iterator.next();
                Sample next = iterator.next();
                base += (next.bytes - base) * (boundary - start) / (double) (next.at - start);
                start = boundary;
            }
            return now <= start ? 0 : (bytes - base) * 1_000_000_000.0 / (now - start);
        }

        private record Sample(long at, long bytes) {}
    }

    record Event(long at, int connectionId, String text) {}


    static final class Tui {
        private static final String ESC = "\u001b[";
        private static final String RESET = ESC + "0m";
        private final Options options;
        private final Metrics metrics;
        private final Object outputLock = new Object();
        private volatile boolean running;
        private ScheduledExecutorService renderer;
        private Thread input;

        Tui(Options options, Metrics metrics) {
            this.options = options;
            this.metrics = metrics;
        }

        void start() {
            running = true;
            synchronized (outputLock) {
                System.out.print(ESC + "2J" + ESC + "H" + ESC + "?25l");
            }
            renderer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("speed-test-tui").factory());
            renderer.scheduleWithFixedDelay(
                () -> {
                    synchronized (outputLock) {
                        if (running) {
                            renderFrame();
                        }
                    }
                }, 0, 200, TimeUnit.MILLISECONDS
            );
            input = Thread.startVirtualThread(this::readInput);
        }

        void finish() {
            running = false;
            if (renderer != null) {
                renderer.shutdownNow();
            }
            if (input != null) {
                input.interrupt();
            }
            synchronized (outputLock) {
                try {
                    // Emit failure logs once after refresh stops, then restore the final diagnostic frame.
                    for (ConnectionSnapshot view : metrics.snapshot(metrics.finishedAt)) {
                        if (view.state == State.FAIL) {
                            logger.warn("connection {} failed at {}: {}", view.id, view.failureStage, view.error);
                        }
                    }
                    if (metrics.workerFailure != null) {
                        logger.warn("speed test worker failed", metrics.workerFailure);
                    }
                    renderFrame();
                } finally {
                    System.out.print(RESET + ESC + "?25h\r\n");
                    System.out.flush();
                }
            }
        }

        private void readInput() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line;
                while (running && (line = reader.readLine()) != null) {
                    acceptInput(line);
                    if (metrics.stopping.isDone()) {
                        return;
                    }
                }
            } catch (IOException e) {
                metrics.event(-1, "input unavailable: " + e);
            }
        }

        void acceptInput(String line) {
            if ("q".equalsIgnoreCase(line.trim())) {
                metrics.requestStop();
            }
        }

        private void renderFrame() {
            System.out.print(frame(Integer.getInteger("terminalColumns", 80), Integer.getInteger("terminalRows", 24)));
            System.out.flush();
        }

        String frame(int columns, int rows) {
            int height = Math.max(1, rows - 1);
            int width = Math.max(1, columns - 1);
            long now = metrics.finishedAt < 0 ? metrics.now() : metrics.finishedAt;
            List<ConnectionSnapshot> views = metrics.snapshot(now);
            ConnectionSnapshot focus = views.stream().min(Comparator.comparingInt(ConnectionSnapshot::priority)
                .thenComparingInt(ConnectionSnapshot::id)).orElse(null);
            boolean finished = metrics.finishedAt >= 0;
            String verdict = finished ? metrics.result(views) : metrics.stopping.isDone() ? "STOPPING" : "RUNNING";
            Color verdictColor = switch (verdict) {
                case "FAILED" -> Color.RED;
                case "PASSED" -> Color.GREEN;
                case "STOPPED", "STOPPING" -> Color.YELLOW;
                default -> Color.CYAN;
            };
            long bytes = views.stream().mapToLong(ConnectionSnapshot::bytes).sum();
            long target = options.payload * options.connections;
            long first = views.stream().mapToLong(ConnectionSnapshot::firstBodyAt).filter(at -> at >= 0).min().orElse(-1);
            long end = views.stream().anyMatch(view -> !view.state.terminal()) ? now
                : views.stream().mapToLong(ConnectionSnapshot::endedAt).max().orElse(now);
            double rx = views.stream().mapToDouble(ConnectionSnapshot::rate).sum();
            String average = first < 0 || end <= first ? "--" : formatBytes((long) (bytes * 1_000_000_000.0 / (end - first))) + "/s";

            List<Line> summary = List.of(
                new Line().add("SpeedTest | ").add(verdictColor, verdict)
                    .add(" " + formatDuration(TimeUnit.NANOSECONDS.toMillis(now)) + " | local proxy download diagnostic"),
                new Line().add("Local <- Proxy " + address(options.proxy) + " <- Peer " + address(options.peer)),
                new Line().add(options.mode.name().toUpperCase(Locale.ROOT) + " | " + options.connections + " connections | "
                    + formatBytes(options.payload) + "/conn" + (options.mode == Mode.throttled ? " | limit " + formatBytes(options.rate) + "/s/conn"
                    : options.mode == Mode.stalled ? " | hold " + options.idleMillis + "ms" : "")),
                new Line().add(Color.GRAY, "RX ").add(Color.CYAN, formatBytes((long) rx) + "/s").add(Color.GRAY, "    AVG ").add(average),
                new Line().add(Color.GRAY, "BODY ").add(progress(bytes, target, width >= 65 ? 24 : 8))
                    .add(" " + formatBytes(bytes) + " / " + formatBytes(target)),
                new Line().add(Color.GRAY, "CONN ").add("active " + count(views, false, null) + "  done "
                    + (count(views, true, State.DONE) + count(views, true, State.OBSERVED)) + "  failed " + count(views, true, State.FAIL)
                    + "  stopped " + count(views, true, State.STOP))
            );
            Line footer = finished ? new Line().add("RESULT ").add(verdictColor, verdict).add(" | local experiment only | q + Enter: exit")
                : new Line().add(verdictColor, verdict).add(Color.GRAY, " | q + Enter: stop");
            List<Line> lines = new ArrayList<>();
            List<Line> details = focusDetails(focus, width);
            if (height < 13) {
                // Keep the verdict, data and failure reason before secondary configuration.
                List<Line> compact = new ArrayList<>(List.of(summary.get(0), summary.get(3), summary.get(4), summary.get(5)));
                compact.addAll(details);
                compact.add(summary.get(2));
                compact.add(summary.get(1));
                lines.addAll(compact.subList(0, Math.min(compact.size(), height - 1)));
            } else {
                lines.addAll(summary);
                lines.add(new Line());
                lines.add(new Line().add(Color.GRAY, " ID  STATE     RX/s          BODY   READ  LAST BODY"));
                int capacity = height - 13;
                boolean hidden = views.size() > capacity;
                int visibleCount = Math.max(0, capacity - (hidden ? 1 : 0));
                List<ConnectionSnapshot> visible = new ArrayList<>(views.subList(0, Math.min(visibleCount, views.size())));
                if (focus != null && !visible.isEmpty() && visible.stream().noneMatch(view -> view.id == focus.id)) {
                    visible.set(visible.size() - 1, focus);
                    visible.sort(Comparator.comparingInt(ConnectionSnapshot::id));
                }
                for (ConnectionSnapshot view : visible) {
                    boolean selected = focus != null && view.id == focus.id;
                    lines.add(new Line().add(selected ? Color.CYAN : Color.DEFAULT, selected ? "*" : " ")
                        .add(String.format(Locale.ROOT, "%3d  ", view.id)).add(stateColor(view.state), String.format(Locale.ROOT, "%-8s", view.state))
                        .add(String.format(
                            Locale.ROOT, "  %-12s %6.1f%%  %-3s   %s", formatBytes((long) view.rate) + "/s",
                            view.bytes * 100.0 / options.payload, view.autoRead ? "on" : "off",
                            view.lastBodyAt < 0 ? "--" : age(now - view.lastBodyAt) + " ago"
                        )));
                }
                if (hidden && capacity > 0) {
                    lines.add(new Line().add(Color.GRAY, "... " + (views.size() - visible.size()) + " connections hidden"));
                } else if (hidden) {
                    lines.set(7, new Line().add(Color.GRAY, " ID  STATE | " + views.size() + " connections hidden"));
                }
                lines.add(new Line());
                lines.addAll(details);
                int eventRows = height - lines.size() - 3;
                if (eventRows > 0) {
                    lines.add(new Line());
                    lines.add(new Line().add(Color.GRAY, "EVENTS"));
                    List<Event> events = metrics.events();
                    for (Event event : events.subList(Math.max(0, events.size() - eventRows), events.size())) {
                        lines.add(new Line().add(
                            Color.GRAY, "+" + formatDuration(TimeUnit.NANOSECONDS.toMillis(event.at)) + " "
                                + (event.connectionId < 0 ? "--" : "#" + event.connectionId) + " " + event.text
                        ));
                    }
                }
            }
            lines.add(footer);
            StringBuilder output = new StringBuilder(ESC + "H");
            for (int index = 0; index < lines.size(); index++) {
                if (index > 0) {
                    output.append("\r\n");
                }
                output.append(lines.get(index).render(width)).append(RESET).append(ESC).append("K");
            }
            return output.append(ESC).append("J").toString();
        }

        private static List<Line> focusDetails(ConnectionSnapshot view, int width) {
            if (view == null) {
                return List.of(new Line().add("FOCUS --"), new Line(), new Line());
            }
            String detail = view.state == State.FAIL ? "failed at " + view.failureStage + ": " + view.error : view.detail;
            if (view.state == State.FAIL && width < 70) {
                return List.of(
                    new Line().add(Color.CYAN, "FOCUS #" + view.id).add(" | failed at ").add(Color.RED, view.failureStage.toString()),
                    new Line().add(Color.RED, view.error), new Line().add("autoRead " + (view.autoRead ? "on" : "off"))
                );
            }
            return List.of(
                new Line().add(Color.CYAN, "FOCUS #" + view.id).add(" | ").add(view.state == State.FAIL ? Color.RED : Color.DEFAULT, detail),
                new Line().add(Color.GRAY, "CONNECT ").add(timing(view.connectNanos)).add(Color.GRAY, " | WARMUP ")
                    .add(timing(view.warmupNanos) + " (configured)").add(Color.GRAY, " | FIRST BODY ")
                    .add(timing(view.firstBodyAt < 0 || view.requestAt < 0 ? -1 : view.firstBodyAt - view.requestAt)),
                new Line().add("autoRead " + (view.autoRead ? "on" : "off") + (view.state == State.HOLD ? " | hold left " : " | resume in ")
                    + timing(view.resumeNanos) + " | pauses " + view.pauseCount + " | paused " + age(view.pausedNanos))
            );
        }

        private static long count(List<ConnectionSnapshot> views, boolean terminal, State state) {
            return views.stream().filter(view -> state == null ? view.state.terminal() == terminal : view.state == state).count();
        }

        private static String address(InetSocketAddress address) {
            return address.getHostString() + ":" + address.getPort();
        }

        private static String timing(long nanos) {
            return nanos < 0 ? "--" : TimeUnit.NANOSECONDS.toMillis(nanos) + "ms";
        }

        private static String age(long nanos) {
            return nanos < TimeUnit.SECONDS.toNanos(1) ? timing(nanos) : String.format(Locale.ROOT, "%.1fs", nanos / 1_000_000_000.0);
        }

        private static String progress(long value, long total, int width) {
            double ratio = total <= 0 ? 0 : Math.clamp(value / (double) total, 0, 1);
            int filled = (int) Math.round(width * ratio);
            return "[" + "#".repeat(filled) + ".".repeat(width - filled) + "] " + String.format(Locale.ROOT, "%.1f%%", ratio * 100);
        }

        private static Color stateColor(State state) {
            return switch (state) {
                case CONNECT, WARMUP, READ -> Color.CYAN;
                case WAIT -> Color.YELLOW;
                case DONE, OBSERVED -> Color.GREEN;
                case FAIL -> Color.RED;
                case STOP -> Color.GRAY;
                case PAUSE, HOLD -> Color.DEFAULT;
            };
        }

        enum Color {
            DEFAULT("0"), CYAN("36"), GREEN("32"), YELLOW("33"), RED("31"), GRAY("90");

            final String code;

            Color(String code) {
                this.code = ESC + code + "m";
            }
        }

        record Span(Color color, String text) {}

        static final class Line {
            final List<Span> spans = new ArrayList<>();

            Line add(String text) {
                return add(Color.DEFAULT, text);
            }

            Line add(Color color, String text) {
                spans.add(new Span(color, text.replaceAll("[\\p{Cc}\\p{Cf}]", "?")));
                return this;
            }

            String render(int width) {
                StringBuilder result = new StringBuilder();
                int remaining = width;
                for (Span span : spans) {
                    StringBuilder clipped = new StringBuilder();
                    for (int offset = 0; offset < span.text.length(); ) {
                        int codePoint = span.text.codePointAt(offset);
                        int cells = cellWidth(codePoint);
                        if (cells > remaining) {
                            return result.append(span.color.code).append(clipped).append(RESET).toString();
                        }
                        clipped.appendCodePoint(codePoint);
                        remaining -= cells;
                        offset += Character.charCount(codePoint);
                    }
                    result.append(span.color.code).append(clipped).append(RESET);
                }
                return result.toString();
            }

            static int cellWidth(int codePoint) {
                int type = Character.getType(codePoint);
                if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK) {
                    return 0;
                }
                return codePoint >= 0x1100 && (codePoint <= 0x115f || codePoint == 0x2329 || codePoint == 0x232a
                    || codePoint >= 0x2e80 && codePoint <= 0xa4cf || codePoint >= 0xac00 && codePoint <= 0xd7a3
                    || codePoint >= 0xf900 && codePoint <= 0xfaff || codePoint >= 0xfe10 && codePoint <= 0xfe19
                    || codePoint >= 0xfe30 && codePoint <= 0xfe6f || codePoint >= 0xff01 && codePoint <= 0xff60
                    || codePoint >= 0xffe0 && codePoint <= 0xffe6 || codePoint >= 0x1f300 && codePoint <= 0x1faff
                    || codePoint >= 0x20000 && codePoint <= 0x3fffd) ? 2 : 1;
            }
        }
    }

    static final class Options {
        InetSocketAddress proxy;
        InetSocketAddress peer;
        int connections = 8;
        long payload = SpeedTestPeerServer.DEFAULT_PAYLOAD;
        Mode mode = Mode.normal;
        long rate;
        long idleMillis = 1_000;
        long relayWarmupMillis = 500;
        final long firstByteTimeoutMillis = 10_000;
        final long completionTimeoutMillis = 300_000;

        static Options fromSystem(InetSocketAddress peer) {
            Options options = new Options();
            String localhost = InetAddress.getLoopbackAddress().getHostName();
            options.proxy = new InetSocketAddress(
                System.getProperty("proxyHost", localhost),
                Integer.parseInt(System.getProperty("proxyPort", "1089"))
            );
            options.peer = peer;
            options.connections = Integer.parseInt(System.getProperty("connections", "8"));
            options.payload = parseSize(System.getProperty("payload", "256m"));
            String rate = System.getProperty("rate", "");
            if (!rate.isBlank()) {
                options.rate = parseSize(rate);
                options.mode = Mode.throttled;
            }
            String mode = System.getProperty("mode");
            if (mode != null) {
                options.mode = Mode.valueOf(mode);
            }
            options.idleMillis = Long.parseLong(System.getProperty("idle", "1000"));
            options.relayWarmupMillis = Long.parseLong(System.getProperty("relayWarmupMillis", "500"));
            return options;
        }

        static long parseSize(String value) {
            String text = value.trim().toLowerCase(Locale.ROOT);
            long multiplier = 1;
            if (text.endsWith("k")) {
                multiplier = 1024;
                text = text.substring(0, text.length() - 1);
            } else if (text.endsWith("m")) {
                multiplier = 1024 * 1024;
                text = text.substring(0, text.length() - 1);
            } else if (text.endsWith("g")) {
                multiplier = 1024L * 1024 * 1024;
                text = text.substring(0, text.length() - 1);
            }
            return Long.parseLong(text) * multiplier;
        }
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fKiB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fMiB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.1fGiB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
