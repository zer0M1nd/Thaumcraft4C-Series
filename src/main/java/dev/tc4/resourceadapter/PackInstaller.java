package dev.tc4.resourceadapter;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

final class PackInstaller {
    static final String CACHE = ".thaumcraftresourceadapter";
    record Result(String source, Path pack, int textures, boolean reused) {
        String id() { return "thaumcraftresourceadapter/" + source; }
    }
    private final Consumer<String> warning;

    PackInstaller(Consumer<String> warning) { this.warning = warning; }

    List<Result> prepare(Path directory, ResourceMapping mapping) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        Path cache = directory.resolve(CACHE);
        List<Path> candidates;
        try (Stream<Path> paths = Files.list(directory)) {
            candidates = paths.filter(p -> !p.equals(cache) && LegacyPack.candidate(p)).sorted().toList();
        }
        var results = new ArrayList<Result>();
        for (Path path : candidates) {
            try (var source = LegacyPack.open(path)) {
                if (source == null) continue;
                var plan = PackConverter.plan(source, mapping);
                if (plan != null) results.add(install(cache, source, plan));
            } catch (IOException | RuntimeException error) {
                warning.accept("Skipping local resource pack '" + path.getFileName() + "': " + error.getMessage());
            }
        }
        return List.copyOf(results);
    }

    private Result install(Path cache, LegacyPack source, PackConverter.Plan plan) throws IOException {
        Files.createDirectories(cache);
        if (!Files.isDirectory(cache, NOFOLLOW_LINKS)) throw new IOException("Invalid adapter cache directory");
        String stem = source.name.replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        if (stem.length() > 40) stem = stem.substring(0, 40);
        Path output = cache.resolve("tc-" + stem + "-" + plan.fingerprint() + ".zip");
        if (PackConverter.valid(output, plan)) return new Result(source.name, output, plan.textures(), true);
        // Content-addressed candidates let a running client keep its previous ZIP open during a reload.
        try (var channel = FileChannel.open(cache.resolve("conversion.lock"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, NOFOLLOW_LINKS); var lock = acquire(channel)) {
            if (Files.isSymbolicLink(output)) throw new IOException("Invalid cache entry: " + output);
            if (PackConverter.valid(output, plan)) return new Result(source.name, output, plan.textures(), true);
            Path candidate = Files.createTempFile(cache, ".converting-", ".tmp");
            try {
                PackConverter.write(candidate, source, plan);
                if (!PackConverter.valid(candidate, plan)) throw new IOException("Generated resource pack failed verification");
                Files.move(candidate, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(candidate);
            }
        }
        return new Result(source.name, output, plan.textures(), false);
    }

    private static FileLock acquire(FileChannel channel) throws IOException {
        long deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos();
        do {
            try {
                var lock = channel.tryLock();
                if (lock != null) return lock;
            } catch (OverlappingFileLockException busy) { /* A second scan is writing in this JVM. */ }
            try { Thread.sleep(50); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted waiting for the adapter cache", interrupted);
            }
        } while (System.nanoTime() < deadline);
        throw new IOException("Timed out waiting for the adapter cache");
    }
}
