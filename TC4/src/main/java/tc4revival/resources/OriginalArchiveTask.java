package tc4revival.resources;

import tc4revival.download.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

final class OriginalArchiveTask implements DownloadQueue.Task {
    private final Path game, directory;
    private final OriginalResourceDownloads.Target target;
    private final DownloadQueue.Item item;
    private final boolean refreshLogo;
    private final Map<URI,String> expected = new LinkedHashMap<>();

    OriginalArchiveTask(Path game, OriginalResourceDownloads.Target target) throws IOException {
        this(game, target, false);
    }
    OriginalArchiveTask(Path game, OriginalResourceDownloads.Target target, boolean checkLogo) throws IOException {
        this(game, target, checkLogo, true);
    }
    OriginalArchiveTask(Path game, OriginalResourceDownloads.Target target, boolean checkLogo, boolean chinese) throws IOException {
        this.game = game;
        this.directory = game.resolve("thaumcraft");
        this.target = target;
        var links = CurseForgeDownload.attempts(target.downloads());
        for (String source : target.downloads()) {
            String hash = target.sources().hashes().getOrDefault(source, target.sha256());
            for (URI link : CurseForgeDownload.attempts(List.of(source))) expected.putIfAbsent(link, hash);
        }
        // An explicit source pin overrides a digest inherited by expanding a mirror alias.
        for (String source : target.downloads()) {
            URI uri = URI.create(source);
            if (expected.containsKey(uri)) expected.put(uri, target.sources().hashes().getOrDefault(source,target.sha256()));
        }
        String id = ProcessedOriginalResources.directoryName(target.legacyModId(), target.minecraftVersion());
        Path cache = directory.resolve(id);
        boolean oldLogo = false;
        if (checkLogo && Files.isDirectory(cache, LinkOption.NOFOLLOW_LINKS))
            try {
                oldLogo = !ProcessedOriginalResources.logoChecked(cache) && target.accepts(target.read(cache).sha256());
            } catch (IOException | RuntimeException ignored) {}
        refreshLogo = oldLogo;
        item = new DownloadQueue.Item(id, target.name() + (refreshLogo ? " · logo" : ""),
                target.legacyModId() + " · MC " + target.minecraftVersion(), directory.toString(),
                refreshLogo ? DownloadQueue.Kind.LOGO : DownloadQueue.Kind.RESOURCE, links,
                ManualLink.ordered(target.sources().manualPages().stream().map(URI::create).toList(), chinese), null);
    }

    public DownloadQueue.Item item() { return item; }

    public DownloadQueue.Prepared local() throws IOException {
        for (Path path : OriginalResources.candidates(directory)) {
            if (refreshLogo && Files.isDirectory(path)) continue;
            try {
                var snapshot = target.read(path);
                if (target.accepts(snapshot.sha256())) return () -> prepare(snapshot);
            } catch (IOException ignored) {}
        }
        return null;
    }

    public DownloadQueue.Prepared dropped(DownloadQueue.DroppedFile file) throws IOException {
        if (!target.accepts(file.sha256())) return null;
        return stage(file.sha256(), temp -> Files.copy(file.path(), temp, StandardCopyOption.REPLACE_EXISTING));
    }

    public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress) throws IOException {
        return prepareDownload(uri, file -> CurseForgeDownload.fetch(uri, file, progress));
    }

    @FunctionalInterface
    interface Fetch { void write(Path temporary) throws IOException; }

    DownloadQueue.Prepared prepareDownload(URI uri, Fetch fetch) throws IOException {
        String hash = expected.get(uri);
        if (hash == null) throw new IOException("No SHA-256 declared for this source");
        return stage(hash, fetch);
    }

    private DownloadQueue.Prepared stage(String expectedHash, Fetch fetch) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".tc4-download-", ".part");
        try {
            fetch.write(temporary);
            var snapshot = target.read(temporary);
            if (!expectedHash.equals(snapshot.sha256())) throw new IOException("SHA-256 mismatch · " + target.name());
            return new DownloadQueue.Prepared() {
                public String apply() throws IOException { return prepare(snapshot); }
                public void close() throws IOException { Files.deleteIfExists(temporary); }
            };
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
    }

    private String prepare(OriginalResources.Snapshot snapshot) throws IOException {
        if (!Files.isDirectory(snapshot.file())) ProcessedOriginalResources.store(game, target, snapshot);
        return "";
    }
}
