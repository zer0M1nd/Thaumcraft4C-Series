package dev.tc4.updater;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** Runs in a private JVM only after the launching game JVM has exited. No Forge classes. */
public final class SelfUpdateHelper {
    static final Gson JSON = new Gson();
    public record Plan(String batch, String version, UpdateTransaction.Entry entry) {}
    static Path pending(Path game) { return game.resolve(".thaumcraftupdater/self-update.json"); }
    static Plan read(Path game) throws IOException {
        Path file = pending(game); FilesSupport.regularFile(file);
        try (var in = Files.newInputStream(file)) {
            Plan plan = JSON.fromJson(new String(FilesSupport.readBounded(in, 1024 * 1024), StandardCharsets.UTF_8), Plan.class);
            if (plan == null || plan.batch == null || !plan.batch.matches("[0-9a-f-]{36}") || plan.entry == null)
                throw new IOException("Invalid self-update plan");
            return plan;
        }
    }
    static boolean installed(Path game, Plan plan) throws IOException {
        Path mods = game.resolve("mods");
        Path target = mods.resolve(FilesSupport.filename(plan.entry.replacement()));
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || !FilesSupport.sha256(target).equals(plan.entry.newHash())) return false;
        for (var old : plan.entry.originals())
            if (!old.filename().equals(plan.entry.replacement()) && Files.exists(mods.resolve(FilesSupport.localFilename(old.filename())), LinkOption.NOFOLLOW_LINKS)) return false;
        return true;
    }
    static void apply(Path game) throws IOException {
        UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            transaction.recover(); transaction.cleanupCompleted(System.out::println);
            if (!Files.exists(pending(game), LinkOption.NOFOLLOW_LINKS)) return;
            Plan plan = read(game);
            if (!installed(game, plan)) {
                Path batch = game.resolve(".thaumcraftupdater/transactions").resolve(plan.batch);
                transaction.commit(batch, List.of(plan.entry));
            }
            Files.delete(pending(game));
            FilesSupport.writeAtomic(game.resolve(".thaumcraftupdater/self-update-result.json"),
                    JSON.toJson(java.util.Map.of("version", plan.version, "status", "applied")).getBytes(StandardCharsets.UTF_8));
            System.out.println("Self-update applied: " + plan.version + "; effective on the next game launch");
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected game directory, parent PID and start time");
        Path game = Path.of(args[0]).toAbsolutePath().normalize();
        long pid = Long.parseLong(args[1]), started = Long.parseLong(args[2]);
        Path helperLock = game.resolve(".thaumcraftupdater/self-helper.lock");
        if (Files.isSymbolicLink(helperLock)) throw new IOException("Unsafe helper lock");
        try (var channel = FileChannel.open(helperLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) return;
            var parent = ProcessHandle.of(pid);
            if (parent.isPresent() && parent.get().info().startInstant().map(t -> t.toEpochMilli() == started).orElse(true))
                parent.get().onExit().join();
            // The process exit closes its ZIP handles and releases the game-directory lock.
            // Never wait on a different game launch; leave the pending plan intact on contention.
            try { apply(game); }
            catch (Exception error) {
                System.err.println("Self-update deferred; pending files retained: " + error);
                error.printStackTrace();
            }
        }
    }
}
