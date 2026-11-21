package dev.tc4.updater;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Journaled batch replacement. Recovery runs before any network access or Forge mod scanning. */
public final class UpdateTransaction {
    private static final Gson JSON = new Gson();
    private final Path mods;
    private final Path state;
    private final Path journal;
    private final Mover mover;

    @FunctionalInterface interface Mover { void move(Path from, Path to) throws IOException; }
    public record Original(String filename, String hash) {}
    public record Entry(String original, String replacement, String oldHash, String newHash, List<Original> duplicates) {
        public Entry(String original, String replacement, String oldHash, String newHash) {
            this(original, replacement, oldHash, newHash, List.of());
        }
        public List<Original> originals() {
            List<Original> result = new ArrayList<>();
            if (original != null) result.add(new Original(original, oldHash));
            if (duplicates != null) result.addAll(duplicates); // Pre-0.2.4 journals have no duplicates field.
            return List.copyOf(result);
        }
    }
    private record Journal(String id, boolean committed, List<Entry> entries) {}
    public static final class StartupBlocked extends RuntimeException {
        public StartupBlocked(String message, Throwable cause) { super(message, cause); }
        public StartupBlocked(String message) { super(message); }
    }
    public record Guard(FileChannel channel, FileLock lock) implements AutoCloseable {
        @Override public void close() throws IOException { try { lock.release(); } finally { channel.close(); } }
    }

    public UpdateTransaction(Path gameDirectory) {
        this(gameDirectory, (from, to) -> Files.move(from, to, StandardCopyOption.ATOMIC_MOVE));
    }
    UpdateTransaction(Path gameDirectory, Mover mover) {
        Path root = gameDirectory.toAbsolutePath().normalize();
        mods = root.resolve("mods");
        state = root.resolve(".thaumcraftupdater");
        journal = state.resolve("active.json");
        this.mover = mover;
    }

    public Guard lock() throws IOException {
        if (Files.isSymbolicLink(mods) || Files.isSymbolicLink(state)) throw new IOException("Symlinked updater/mods directories are unsupported");
        Files.createDirectories(state);
        Path lockPath = state.resolve("update.lock");
        if (Files.isSymbolicLink(lockPath)) throw new IOException("Symlinked updater lock");
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new StartupBlocked("Another process is updating this game directory. Close that launch before retrying.");
            return new Guard(channel, lock);
        } catch (OverlappingFileLockException error) {
            channel.close();
            throw new StartupBlocked("Another updater holds this game directory", error);
        } catch (RuntimeException | IOException error) {
            channel.close();
            throw error;
        }
    }

    public Path newBatch() throws IOException {
        Path batches = state.resolve("transactions");
        if (Files.isSymbolicLink(batches)) throw new IOException("Symlinked transaction directory");
        Path batch = batches.resolve(UUID.randomUUID().toString());
        Files.createDirectories(batch.resolve("staged"));
        Files.createDirectory(batch.resolve("originals"));
        return batch;
    }
    public static Path staged(Path batch, int index) { return batch.resolve("staged/" + index + ".jar"); }
    private static Path backup(Path batch, int index) { return batch.resolve("originals/" + index + ".jar"); }
    private static Path backup(Path batch, int entry, int original) {
        return original == 0 ? backup(batch, entry) : batch.resolve("originals/" + entry + "-" + original + ".jar");
    }

    public void recover() {
        if (!Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            FilesSupport.regularFile(journal);
            Journal saved = readJournal(journal);
            Path batch = validate(saved);
            if (!saved.committed) rollback(saved, batch);
            archive(saved, batch);
        } catch (IOException | RuntimeException error) {
            throw new StartupBlocked("Cannot recover an interrupted mod update. Preserve .thaumcraftupdater and resolve the reported file conflict before launching.", error);
        }
    }

    public void commit(Path batch, List<Entry> entries) throws IOException {
        Journal saved = new Journal(batch.getFileName().toString(), false, List.copyOf(entries));
        if (!batch.equals(validate(saved))) throw new IOException("Invalid staging directory");
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            for (Original original : entry.originals()) requireHash(mods.resolve(original.filename), original.hash);
            requireHash(staged(batch, i), entry.newHash);
            Path destination = mods.resolve(entry.replacement);
            if (entry.originals().stream().noneMatch(old -> old.filename.equals(entry.replacement)) && Files.exists(destination, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Destination already exists: " + destination);
        }
        FilesSupport.writeAtomic(journal, JSON.toJson(saved).getBytes(StandardCharsets.UTF_8));
        try {
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                List<Original> originals = entry.originals();
                for (int j = 0; j < originals.size(); j++) mover.move(mods.resolve(originals.get(j).filename), backup(batch, i, j));
                mover.move(staged(batch, i), mods.resolve(entry.replacement));
            }
            Journal committed = new Journal(saved.id, true, saved.entries);
            // This durable commit marker prevents a later launch from undoing a successful update.
            FilesSupport.writeAtomic(journal, JSON.toJson(committed).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            try { rollback(saved, batch); archive(saved, batch); }
            catch (IOException | RuntimeException recoveryFailure) {
                recoveryFailure.addSuppressed(failure);
                throw new StartupBlocked("Mod replacement failed and rollback could not finish; launch stopped to prevent mixed versions.", recoveryFailure);
            }
            throw new IOException("Update rolled back; original mods restored", failure);
        }
        // Leave a committed journal for the next launch if receipt cleanup fails; never roll back it.
        try { archive(new Journal(saved.id, true, saved.entries), batch); }
        catch (IOException ignored) { /* recover() safely finalizes it on the next launch. */ }
    }

    private Path validate(Journal saved) throws IOException { return validate(saved, true); }

    private Path validate(Journal saved, boolean requireWorkingDirectories) throws IOException {
        if (saved == null || saved.id == null || !saved.id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || saved.entries == null || saved.entries.isEmpty() || saved.entries.size() > 64)
            throw new IOException("Invalid transaction journal");
        Path batch = state.resolve("transactions").resolve(saved.id);
        for (Path path : List.of(state, state.resolve("transactions"), batch)) {
            if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe transaction directory: " + path);
        }
        for (Path path : List.of(batch.resolve("originals"), batch.resolve("staged"))) {
            if ((requireWorkingDirectories || Files.exists(path, LinkOption.NOFOLLOW_LINKS))
                    && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe transaction directory: " + path);
        }
        Map<String, Entry> originals = new HashMap<>();
        Set<String> replacements = new HashSet<>();
        for (Entry entry : saved.entries) {
            if (entry == null) throw new IOException("Missing transaction entry");
            if (entry.original != null) FilesSupport.localFilename(entry.original);
            FilesSupport.filename(entry.replacement);
            if ((entry.original == null) != (entry.oldHash == null) || entry.newHash == null
                    || (entry.oldHash != null && !entry.oldHash.matches("[a-f0-9]{64}")) || !entry.newHash.matches("[a-f0-9]{64}"))
                throw new IOException("Invalid transaction hashes");
            if (entry.originals().size() > 128 || (entry.original == null && !entry.originals().isEmpty()))
                throw new IOException("Invalid duplicate originals");
            for (Original old : entry.originals()) {
                if (old == null || old.hash == null || !old.hash.matches("[a-f0-9]{64}")) throw new IOException("Invalid original hash");
                FilesSupport.localFilename(old.filename);
                if (originals.putIfAbsent(old.filename.toLowerCase(Locale.ROOT), entry) != null)
                    throw new IOException("Duplicate transaction filename");
            }
            if (!replacements.add(entry.replacement.toLowerCase(Locale.ROOT))) throw new IOException("Duplicate transaction filename");
        }
        for (Entry entry : saved.entries) {
            if (originals.containsKey(entry.replacement.toLowerCase(Locale.ROOT)) && originals.get(entry.replacement.toLowerCase(Locale.ROOT)) != entry)
                throw new IOException("Cross-mod replacement filename collision");
        }
        return batch;
    }

    private void rollback(Journal saved, Path batch) throws IOException {
        // Validate the entire recovery before removing or restoring a single file.
        for (int i = 0; i < saved.entries.size(); i++) {
            Entry entry = saved.entries.get(i);
            Path replacement = mods.resolve(entry.replacement);
            List<Original> originals = entry.originals();
            for (int j = 0; j < originals.size(); j++) {
                Original old = originals.get(j);
                Path original = mods.resolve(old.filename), backup = backup(batch, i, j);
                boolean hasBackup = Files.exists(backup, LinkOption.NOFOLLOW_LINKS);
                if (hasBackup) requireHash(backup, old.hash);
                if (Files.exists(original, LinkOption.NOFOLLOW_LINKS)) {
                    String hash = checkedHash(original);
                    if (!hash.equals(old.hash) && !(hasBackup && original.equals(replacement) && hash.equals(entry.newHash)))
                        throw new IOException("Original file changed outside updater: " + original);
                } else if (!hasBackup) throw new IOException("Missing both original and backup: " + original);
            }
            if (originals.stream().noneMatch(old -> old.filename.equals(entry.replacement))
                    && Files.exists(replacement, LinkOption.NOFOLLOW_LINKS)) requireHash(replacement, entry.newHash);
        }
        for (int i = saved.entries.size() - 1; i >= 0; i--) {
            Entry entry = saved.entries.get(i);
            Path replacement = mods.resolve(entry.replacement);
            List<Original> originals = entry.originals();
            if (Files.exists(replacement) && checkedHash(replacement).equals(entry.newHash)
                    && originals.stream().noneMatch(old -> old.filename.equals(entry.replacement) && old.hash.equals(entry.newHash)))
                Files.delete(replacement);
            for (int j = originals.size() - 1; j >= 0; j--) {
                Original old = originals.get(j); Path original = mods.resolve(old.filename);
                if (!Files.exists(original)) Files.move(backup(batch, i, j), original, StandardCopyOption.ATOMIC_MOVE);
                requireHash(original, old.hash);
            }
        }
    }

    private void archive(Journal saved, Path batch) throws IOException {
        FilesSupport.writeAtomic(batch.resolve(saved.committed ? "committed.json" : "rolled-back.json"), JSON.toJson(saved).getBytes(StandardCharsets.UTF_8));
        Files.delete(journal);
        // The durable outcome is recorded and no recovery needs these files anymore.
        // If cleanup is interrupted/locked, the receipt lets the next launch finish it safely.
        try { cleanupBatch(saved, batch); }
        catch (IOException ignored) { /* cleanupCompleted retries without undoing a completed update. */ }
    }

    /** Also reclaims completed backups from older updater versions, never an active recovery. */
    public void cleanupCompleted(Consumer<String> log) {
        if (Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) return;
        Path batches = state.resolve("transactions");
        if (!Files.exists(batches, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (!Files.isDirectory(batches, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe transactions directory");
            List<Path> paths;
            try (var files = Files.list(batches)) { paths = files.toList(); }
            for (Path batch : paths) {
                if (!batch.getFileName().toString().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) continue;
                try {
                    if (!Files.isDirectory(batch, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe transaction directory: " + batch);
                    Path receipt = batch.resolve("committed.json");
                    if (!Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) receipt = batch.resolve("rolled-back.json");
                    if (!Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) {
                        try (var files = Files.list(batch)) {
                            if (files.findAny().isEmpty()) Files.delete(batch);
                        }
                        continue;
                    }
                    Journal saved = readJournal(receipt);
                    if (!receipt.getFileName().toString().equals(saved.committed ? "committed.json" : "rolled-back.json"))
                        throw new IOException("Receipt outcome mismatch");
                    cleanupBatch(saved, batch);
                } catch (IOException | RuntimeException error) { log.accept("Deferred transaction cleanup: " + error.getMessage()); }
            }
        } catch (IOException | RuntimeException error) { log.accept("Deferred transaction cleanup: " + error.getMessage()); }
    }

    private static Journal readJournal(Path path) throws IOException {
        FilesSupport.regularFile(path);
        try (var input = Files.newInputStream(path)) {
            return JSON.fromJson(new String(FilesSupport.readBounded(input, 1024 * 1024), StandardCharsets.UTF_8), Journal.class);
        }
    }

    private void cleanupBatch(Journal saved, Path batch) throws IOException {
        if (!batch.equals(validate(saved, false))) throw new IOException("Receipt directory mismatch");
        String receiptName = saved.committed ? "committed.json" : "rolled-back.json";
        Map<Path, String> expected = new LinkedHashMap<>();
        for (int i = 0; i < saved.entries.size(); i++) {
            Entry entry = saved.entries.get(i);
            List<Original> originals = entry.originals();
            for (int j = 0; j < originals.size(); j++) expected.put(backup(batch, i, j), originals.get(j).hash);
            expected.put(staged(batch, i), entry.newHash);
        }
        // Check the complete, bounded set before deleting; never recurse into unknown files or links.
        Set<String> allowed = Set.of("originals", "staged", receiptName);
        try (var files = Files.list(batch)) {
            for (Path path : files.toList()) if (!allowed.contains(path.getFileName().toString()))
                throw new IOException("Unexpected transaction file: " + path);
        }
        Path receipt = batch.resolve(receiptName);
        FilesSupport.regularFile(receipt);
        for (String folder : List.of("originals", "staged")) {
            Path directory = batch.resolve(folder);
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) continue;
            try (var files = Files.list(directory)) {
                for (Path path : files.toList()) {
                    if (!expected.containsKey(path)) throw new IOException("Unexpected transaction file: " + path);
                    requireHash(path, expected.get(path));
                }
            }
        }
        for (Path path : expected.keySet()) Files.deleteIfExists(path);
        Files.deleteIfExists(batch.resolve("originals"));
        Files.deleteIfExists(batch.resolve("staged"));
        Files.delete(receipt);
        Files.delete(batch);
    }

    /** Dispose a download/precheck failure only if there is no recovery journal and no original backup. */
    public void discardStaged(Path batch, int count, Consumer<String> log) {
        if (Files.exists(journal, LinkOption.NOFOLLOW_LINKS) || !Files.exists(batch, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (!batch.getParent().equals(state.resolve("transactions"))
                    || !Files.isDirectory(batch, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe staging directory");
            // A finished batch, including an interrupted cleanup, is handled from its receipt instead.
            if (Files.exists(batch.resolve("committed.json")) || Files.exists(batch.resolve("rolled-back.json"))) return;
            for (String directory : List.of("originals", "staged"))
                if (!Files.isDirectory(batch.resolve(directory), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe staging subdirectory");
            try (var files = Files.list(batch.resolve("originals"))) {
                if (files.findAny().isPresent()) throw new IOException("Original backups still require recovery");
            }
            try (var files = Files.list(batch)) {
                if (files.anyMatch(path -> !Set.of("originals", "staged").contains(path.getFileName().toString())))
                    throw new IOException("Unexpected staging file");
            }
            Set<Path> expected = new HashSet<>();
            for (int i = 0; i < count; i++) expected.add(staged(batch, i));
            try (var files = Files.list(batch.resolve("staged"))) {
                for (Path path : files.toList()) {
                    if (!expected.contains(path)) throw new IOException("Unexpected staged download");
                    FilesSupport.regularFile(path);
                }
            }
            for (Path path : expected) Files.deleteIfExists(path);
            Files.delete(batch.resolve("staged")); Files.delete(batch.resolve("originals")); Files.delete(batch);
        } catch (IOException | RuntimeException error) { log.accept("Could not discard staged downloads: " + error.getMessage()); }
    }
    private static String checkedHash(Path path) throws IOException { FilesSupport.regularFile(path); return FilesSupport.sha256(path); }
    private static void requireHash(Path path, String expected) throws IOException {
        if (!checkedHash(path).equals(expected)) throw new IOException("File changed or checksum failed: " + path);
    }
}
