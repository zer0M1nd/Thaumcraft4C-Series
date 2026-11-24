package dev.tc4.updater;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

/** Stages self replacement without touching any loaded service JAR. */
public final class SelfUpdate {
    public static void stage(Path game, String version, UpdateTransaction.Entry entry, Path downloaded) throws IOException {
        if (Files.exists(SelfUpdateHelper.pending(game), LinkOption.NOFOLLOW_LINKS)) {
            var previous = SelfUpdateHelper.read(game);
            if (SelfUpdateHelper.installed(game, previous)) Files.delete(SelfUpdateHelper.pending(game));
            else if (previous.entry().equals(entry) && Version.parse(previous.version()).equals(Version.parse(version))) {
                Path staged = UpdateTransaction.staged(game.resolve(".thaumcraftupdater/transactions").resolve(previous.batch()), 0);
                if (Files.isRegularFile(staged, LinkOption.NOFOLLOW_LINKS) && FilesSupport.sha256(staged).equals(entry.newHash())) return;
                if (Files.exists(staged, LinkOption.NOFOLLOW_LINKS) || Files.exists(game.resolve(".thaumcraftupdater/active.json")))
                    throw new IOException("Pending self-update requires recovery; evidence retained");
                Files.delete(SelfUpdateHelper.pending(game)); // A fully rolled-back attempt may be downloaded/staged again.
            } else throw new IOException("A different self-update is already pending; finish or resolve it first");
        }
        for (var old : entry.originals()) {
            Path path = game.resolve("mods").resolve(FilesSupport.localFilename(old.filename()));
            FilesSupport.regularFile(path);
            if (!FilesSupport.sha256(path).equals(old.hash())) throw new IOException("Self-update original changed: " + old.filename());
        }
        UpdateTransaction transaction = new UpdateTransaction(game);
        Path batch = transaction.newBatch();
        try {
            Files.copy(downloaded, UpdateTransaction.staged(batch, 0));
            if (!FilesSupport.sha256(UpdateTransaction.staged(batch, 0)).equals(entry.newHash())) throw new IOException("Self-update staged checksum mismatch");
            var plan = new SelfUpdateHelper.Plan(batch.getFileName().toString(), version, entry);
            FilesSupport.writeAtomic(SelfUpdateHelper.pending(game), SelfUpdateHelper.JSON.toJson(plan).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException error) {
            transaction.discardStaged(batch, 1, ignored -> {}); throw error;
        }
    }
    public static void launch(Path game, Consumer<String> log) {
        if (!Files.exists(SelfUpdateHelper.pending(game), LinkOption.NOFOLLOW_LINKS)) return;
        try {
            SelfUpdateHelper.read(game);
            byte[] bytes;
            try (var in = SelfUpdate.class.getResourceAsStream("/META-INF/thaumcraftupdater/self-update-helper.jar")) {
                if (in == null) throw new IOException("Missing self-update helper");
                bytes = FilesSupport.readBounded(in, 4 * 1024 * 1024);
            }
            Path directory = game.resolve(".thaumcraftupdater/helper");
            if (Files.isSymbolicLink(directory)) throw new IOException("Unsafe helper directory");
            Files.createDirectories(directory);
            String hash = FilesSupport.sha256(bytes); Path helper = directory.resolve(hash + ".jar");
            if (!Files.exists(helper, LinkOption.NOFOLLOW_LINKS)) FilesSupport.writeAtomic(helper, bytes);
            FilesSupport.regularFile(helper);
            if (!FilesSupport.sha256(helper).equals(hash)) throw new IOException("Self-update helper checksum mismatch");
            String executable = System.getProperty("os.name").startsWith("Windows") ? "javaw.exe" : "java";
            Path java = Path.of(System.getProperty("java.home"), "bin", executable);
            var parent = ProcessHandle.current();
            new ProcessBuilder(java.toString(), "-jar", helper.toString(), game.toAbsolutePath().toString(),
                    Long.toString(parent.pid()), Long.toString(parent.info().startInstant().orElseThrow().toEpochMilli()))
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(game.resolve(".thaumcraftupdater/self-update.log").toFile())).start();
            log.accept("Self-update staged; helper will replace all old updater JARs after game exit; next launch uses the new version");
        } catch (Exception error) { log.accept("Could not start self-update helper; pending update retained: " + error.getMessage()); }
    }
}
