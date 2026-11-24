package dev.tc4.download;

import com.google.gson.Gson;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.*;
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
        THIRD_PARTY_INSTALL,
        OPTIONAL,
        LOGO
    }

    public record Item(
            String id,
            String name,
            String detail,
            String directory,
            Kind kind,
            List<URI> links,
            List<ManualLink> manualLinks,
            String changelog) {
        public Item(
                String id,
                String name,
                String detail,
                String directory,
                Kind kind,
                List<URI> links) {
            this(id, name, detail, directory, kind, links, List.of(), null);
        }

        public Item {
            links = List.copyOf(new LinkedHashSet<>(links));
            manualLinks = List.copyOf(manualLinks);
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
        default Prepared local() throws IOException { return null; }

        /** Only user-selected bytes may satisfy this path. Return null for another item's file. */
        default Prepared dropped(DroppedFile file) throws IOException { return null; }

        /** A local discovery retry or a catalog check can have no artifact URLs. */
        default Prepared check() throws IOException { return null; }

        default List<Task> followUp() { return List.of(); }

        Prepared download(URI link, Progress progress) throws IOException;
    }

    public record DroppedFile(Path path, long size, String sha256, String md5) {
        static DroppedFile read(Path path) throws IOException {
            final long limit = 128L * 1024 * 1024;
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > limit)
                throw new IOException("Expected a regular file of at most 128 MiB");
            try (var in = Files.newInputStream(path)) {
                var sha = MessageDigest.getInstance("SHA-256");
                var md5 = MessageDigest.getInstance("MD5");
                byte[] buffer = new byte[65536];
                long size = 0;
                for (int n; (n = in.read(buffer)) != -1;) {
                    size += n;
                    if (size > limit) throw new IOException("File exceeds size limit");
                    sha.update(buffer, 0, n);
                    md5.update(buffer, 0, n);
                }
                return new DroppedFile(path, size, HexFormat.of().formatHex(sha.digest()), HexFormat.of().formatHex(md5.digest()));
            } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        }
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
        boolean running, failed;
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
    private final ExecutorService drops = Executors.newSingleThreadExecutor(r -> daemon(r, "Startup file drop"));
    private int pendingDrops;
    private String dropMessage = "";
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
        return pendingDrops == 0 && rows.stream().allMatch(r -> r.state == State.DONE || omitted(r));
    }

    public synchronized boolean skipped(String id) {
        return rows.stream().anyMatch(r -> r.item.id().equals(id) && omitted(r));
    }

    private static boolean omitted(Row r) {
        return r.state == State.FAILED || r.state == State.SKIPPED;
    }

    private static boolean issue(Row r) {
        return omitted(r) && (r.failed || r.item.kind() == Kind.INSTALL || r.item.kind() == Kind.THIRD_PARTY_INSTALL || r.item.kind() == Kind.RESOURCE);
    }

    public synchronized boolean needsConfirmation() {
        return rows.stream().anyMatch(r -> issue(r) && r.item.kind() != Kind.OPTIONAL);
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
                            "issue", issue(r),
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
                        "dropMessage", dropMessage,
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
                schedule();
            }
            return;
        }
        if (action.equals("drop")) {
            manual = true;
            pendingDrops++;
            drops.execute(() -> acceptDrop(id));
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
                        manual = true;
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
                    if ((r.item.kind() != Kind.RESOURCE || r.state == State.FAILED || r.state == State.CANCELLED)
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
        if (closed || !started || pendingDrops > 0) return;
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
            if (r.item.links().isEmpty()) {
                try (Prepared result = r.task.check()) {
                    if (result != null) {
                        if (reserve(r, generation, false)) finish(r, result);
                        return;
                    }
                }
            }
            throw new IOException(errors.isEmpty() ? "没有可自动下载的地址" : String.join(" / ", errors));
        } catch (IOException | RuntimeException ex) {
            synchronized (this) {
                if (!closed && generation == r.generation) {
                    r.message = reason(ex);
                    r.state = State.FAILED;
                    r.failed = true;
                    LOG.log(System.Logger.Level.WARNING, r.item.name() + ": " + r.message);
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
                r.failed = false;
            }
        } catch (IOException | RuntimeException ex) {
            synchronized (this) {
                r.message = reason(ex);
                r.state = State.FAILED;
                r.failed = true;
                LOG.log(System.Logger.Level.WARNING, r.item.name() + ": " + r.message);
                manual = true;
            }
        }
    }

    private void acceptDrop(String path) {
        boolean matched = false;
        try {
            var file = DroppedFile.read(Path.of(path));
            for (var r : rows) {
                int generation;
                synchronized (this) {
                    if (closed) return;
                    if (r.state == State.DONE || r.state == State.PREPARING) continue;
                    generation = r.generation;
                }
                try (var local = r.task.dropped(file)) {
                    if (local != null && reserve(r, generation, true)) {
                        finish(r, local);
                        matched = true;
                    }
                }
            }
            synchronized (this) { dropMessage = matched ? "" : "哈希与待下载项目不匹配 / No pending item matches this file's checksum"; }
        } catch (IOException | RuntimeException e) {
            synchronized (this) { dropMessage = reason(e); }
        } finally {
            synchronized (this) { pendingDrops--; schedule(); }
        }
    }

    public synchronized void reject(String message) {
        manual = true;
        for (var row : rows)
            if (row.state == State.DONE) {
                row.state = State.FAILED;
                row.failed = true;
                row.message = message;
            }
    }

    public void awaitWorkers() throws IOException {
        try {
            if (!transfers.awaitTermination(15, TimeUnit.SECONDS)
                    || !drops.awaitTermination(15, TimeUnit.SECONDS))
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
        drops.shutdownNow();
    }
}
