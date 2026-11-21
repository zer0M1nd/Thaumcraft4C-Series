package dev.tc4.download;

import com.google.gson.Gson;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * A batch starts only after the first visible frame. All filesystem and network work runs off the
 * UI thread.
 */
public final class DownloadQueue implements AutoCloseable {
    public enum Kind {
        RESOURCE,
        UPDATE,
        INSTALL,
        OPTIONAL
    }

    public record Item(
            String id,
            String name,
            String detail,
            String directory,
            Kind kind,
            List<URI> links,
            String changelog) {
        public Item(
                String id,
                String name,
                String detail,
                String directory,
                Kind kind,
                List<URI> links) {
            this(id, name, detail, directory, kind, links, null);
        }

        public Item {
            links = List.copyOf(new LinkedHashSet<>(links));
        }
    }

    @FunctionalInterface
    public interface Progress {
        void bytes(long received, long total) throws IOException;
    }

    public interface Prepared extends AutoCloseable {
        String apply() throws IOException;

        default void close() throws IOException {}
    }

    public interface Task {
        Item item();

        /**
         * Return null if the player has not supplied a usable file. Never access the network here.
         */
        Prepared local() throws IOException;

        Prepared download(URI link, Progress progress) throws IOException;
    }

    private enum State {
        QUEUED,
        ACTIVE,
        PAUSED,
        CANCELLED,
        FAILED,
        PREPARING,
        DONE,
        SKIPPED
    }

    private static final class Row {
        final Task task;
        final Item item;
        State state = State.QUEUED;
        long received, total = -1;
        int source, generation;
        boolean running;
        String message = "";

        Row(Task task) {
            this.task = task;
            this.item = task.item();
        }
    }

    private static final System.Logger LOG = System.getLogger("ThaumcraftDownloads");
    private final List<Row> rows;
    private final ExecutorService transfers =
            Executors.newCachedThreadPool(r -> daemon(r, "Startup download"));
    private final ScheduledExecutorService detector =
            Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "Startup file check"));
    private final Gson json = new Gson();
    private boolean started, manual, closed;

    public DownloadQueue(List<? extends Task> tasks) {
        rows = tasks.stream().map(Row::new).toList();
    }

    private static Thread daemon(Runnable work, String name) {
        var thread = new Thread(work, name);
        thread.setDaemon(true);
        return thread;
    }

    public synchronized boolean ready() {
        return rows.stream().allMatch(r -> r.state == State.DONE || r.state == State.SKIPPED);
    }

    public synchronized boolean skipped(String id) {
        return rows.stream().anyMatch(r -> r.item.id().equals(id) && r.state == State.SKIPPED);
    }

    public synchronized boolean needsConfirmation() {
        return rows.stream()
                .anyMatch(r -> r.state == State.SKIPPED && r.item.kind() == Kind.INSTALL);
    }

    public synchronized String snapshot() {
        var entries = new ArrayList<Map<String, Object>>();
        for (var r : rows)
            entries.add(
                    Map.of(
                            "item",
                            r.item,
                            "state",
                            r.state,
                            "received",
                            r.received,
                            "total",
                            r.total,
                            "source",
                            r.source,
                            "message",
                            r.message));
        return json.toJson(
                Map.of(
                        "rows",
                        entries,
                        "ready",
                        ready(),
                        "auto",
                        started && !manual && ready(),
                        "confirm",
                        needsConfirmation()));
    }

    public synchronized void action(String action, String id) {
        if (closed) return;
        if (action.equals("shown")) {
            if (!started) {
                started = true;
                LOG.log(
                        System.Logger.Level.INFO,
                        "Startup download window visible ({0} items)",
                        rows.size());
                detector.scheduleAtFixedRate(this::checkFiles, 5, 5, TimeUnit.SECONDS);
                schedule();
            }
            return;
        }
        if (!started) return;
        for (var r : rows) {
            if (!id.isEmpty() && !r.item.id().equals(id)) continue;
            switch (action) {
                case "pause" -> {
                    if (r.state == State.ACTIVE || r.state == State.QUEUED) {
                        r.state = State.PAUSED;
                        manual = true;
                    }
                }
                case "resume" -> {
                    if (r.state == State.PAUSED) r.state = r.running ? State.ACTIVE : State.QUEUED;
                }
                case "retry" -> {
                    if (r.state == State.FAILED
                            || r.state == State.CANCELLED
                            || r.state == State.SKIPPED) {
                        r.state = State.QUEUED;
                        r.message = "";
                        r.received = 0;
                        r.total = -1;
                    }
                }
                case "cancel" -> {
                    if (r.state == State.ACTIVE
                            || r.state == State.PAUSED
                            || r.state == State.QUEUED) {
                        r.generation++;
                        r.running = false;
                        r.state = State.CANCELLED;
                        manual = true;
                    }
                }
                case "skip" -> {
                    if (r.item.kind() != Kind.RESOURCE
                            && r.state != State.DONE
                            && r.state != State.PREPARING) {
                        r.generation++;
                        r.running = false;
                        r.state = State.SKIPPED;
                        manual = true;
                    }
                }
                default -> {}
            }
        }
        notifyAll();
        schedule();
    }

    private void schedule() {
        if (closed) return;
        int active =
                (int)
                        rows.stream()
                                .filter(r -> r.state == State.ACTIVE || r.state == State.PREPARING)
                                .count();
        for (var r : rows) {
            if (active >= 3) break;
            if (r.state != State.QUEUED) continue;
            r.state = State.ACTIVE;
            r.running = true;
            int generation = ++r.generation;
            transfers.execute(() -> transfer(r, generation));
            active++;
        }
    }

    private synchronized void progress(Row r, int generation, long received, long total)
            throws IOException {
        while (!closed && r.generation == generation && r.state == State.PAUSED) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
        }
        if (closed || r.generation != generation || r.state != State.ACTIVE)
            throw new InterruptedIOException("Cancelled");
        r.received = received;
        r.total = total;
    }

    private void transfer(Row r, int generation) {
        var errors = new ArrayList<String>();
        try {
            for (int i = 0; i < r.item.links().size(); i++) {
                URI link = r.item.links().get(i);
                progress(r, generation, 0, -1);
                synchronized (this) {
                    if (r.generation != generation) return;
                    r.source = i;
                }
                LOG.log(
                        System.Logger.Level.INFO,
                        "Downloading {0} from {1}",
                        r.item.name(),
                        link.getHost());
                Prepared candidate;
                try {
                    candidate =
                            r.task.download(
                                    link, (bytes, total) -> progress(r, generation, bytes, total));
                } catch (IOException | RuntimeException ex) {
                    synchronized (this) {
                        if (closed || r.generation != generation) return;
                    }
                    LOG.log(System.Logger.Level.DEBUG, "Source failed: " + link.getHost(), ex);
                    errors.add(link.getHost() + " · " + reason(ex));
                    continue;
                }
                try (candidate) {
                    if (reserve(r, generation, false)) finish(r, candidate);
                }
                return;
            }
            throw new IOException(errors.isEmpty() ? "没有可自动下载的地址" : String.join(" / ", errors));
        } catch (IOException | RuntimeException ex) {
            synchronized (this) {
                if (!closed && generation == r.generation) {
                    r.message = reason(ex);
                    r.state = State.FAILED;
                    manual = true;
                }
            }
        } finally {
            synchronized (this) {
                if (generation == r.generation) r.running = false;
                schedule();
            }
        }
    }

    private static String reason(Exception ex) {
        return Objects.toString(ex.getMessage(), ex.getClass().getSimpleName());
    }

    private synchronized boolean reserve(Row r, int generation, boolean local) throws IOException {
        if (!local) progress(r, generation, r.received, r.total);
        if (closed
                || generation != r.generation
                || r.state == State.PREPARING
                || r.state == State.DONE) return false;
        if (local) {
            r.generation++;
            r.running = false;
            manual = true;
            notifyAll();
        }
        r.state = State.PREPARING;
        return true;
    }

    private void finish(Row r, Prepared candidate) {
        try {
            String warning = candidate.apply();
            synchronized (this) {
                r.message = Objects.toString(warning, "");
                r.state = State.DONE;
            }
        } catch (IOException | RuntimeException ex) {
            synchronized (this) {
                r.message = reason(ex);
                r.state = State.FAILED;
                manual = true;
            }
        }
    }

    private void checkFiles() {
        for (var r : rows) {
            int generation;
            synchronized (this) {
                if (closed) return;
                if (r.state == State.DONE || r.state == State.PREPARING) continue;
                generation = r.generation;
            }
            try (var local = r.task.local()) {
                if (local != null && reserve(r, generation, true)) finish(r, local);
            } catch (IOException | RuntimeException ignored) {
                /* The player may still be copying the file. */
            }
        }
        synchronized (this) {
            schedule();
        }
    }

    public synchronized void reject(String message) {
        manual = true;
        for (var row : rows)
            if (row.state == State.DONE) {
                row.state = State.FAILED;
                row.message = message;
            }
    }

    public void awaitWorkers() throws IOException {
        try {
            if (!transfers.awaitTermination(15, TimeUnit.SECONDS)
                    || !detector.awaitTermination(15, TimeUnit.SECONDS))
                throw new IOException("Download workers are still releasing temporary files");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Download cleanup interrupted");
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
        transfers.shutdownNow();
        detector.shutdownNow();
    }
}
