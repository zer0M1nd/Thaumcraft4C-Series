package tc4revival.resources;

import tc4revival.download.DownloadQueue;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

final class OriginalArchiveTask implements DownloadQueue.Task {
    private final Path game, directory;
    private final OriginalResourceDownloads.Target target;
    private final DownloadQueue.Item item;
    private final boolean refreshLogo;

    OriginalArchiveTask(Path game, OriginalResourceDownloads.Target target) throws IOException {
        this(game, target, false);
    }

    OriginalArchiveTask(Path game, OriginalResourceDownloads.Target target, boolean checkLogo)
            throws IOException {
        this.game = game;
        this.directory = game.resolve("thaumcraft");
        this.target = target;
        var sources = new LinkedHashSet<>(target.downloads());
        sources.add(target.page());
        var links = new LinkedHashSet<>(CurseForgeDownload.attempts(List.copyOf(sources)));
        for (var source : sources) {
            try {
                var uri = URI.create(source);
                if (CurseForgeDownload.webLink(uri)) links.add(uri);
            } catch (IllegalArgumentException ignored) {
            }
        }
        String id =
                ProcessedOriginalResources.directoryName(
                        target.legacyModId(), target.minecraftVersion());
        Path cache = directory.resolve(id);
        boolean oldLogo = false;
        if (checkLogo && Files.isDirectory(cache, LinkOption.NOFOLLOW_LINKS))
            try {
                if (!ProcessedOriginalResources.logoChecked(cache))
                    oldLogo = target.read(cache).sha256().equals(target.sha256());
            } catch (IOException | RuntimeException ignored) {
            }
        refreshLogo = oldLogo;
        item =
                new DownloadQueue.Item(
                        id,
                        target.name() + (refreshLogo ? " · logo" : ""),
                        target.legacyModId() + " · MC " + target.minecraftVersion(),
                        directory.toString(),
                        refreshLogo ? DownloadQueue.Kind.OPTIONAL : DownloadQueue.Kind.RESOURCE,
                        List.copyOf(links));
    }

    @Override
    public DownloadQueue.Item item() {
        return item;
    }

    @Override
    public DownloadQueue.Prepared local() throws IOException {
        OriginalResources.Snapshot firstVariant = null;
        for (Path path : OriginalResources.candidates(directory)) {
            if (Files.isDirectory(path) && refreshLogo) continue;
            OriginalResources.Snapshot snapshot;
            try {
                snapshot = target.read(path);
            } catch (IOException e) {
                continue;
            }
            if (snapshot.sha256().equals(target.sha256())) return () -> prepare(snapshot);
            if (firstVariant == null) firstVariant = snapshot;
        }
        var selected = firstVariant;
        return selected == null ? null : () -> prepare(selected);
    }

    @Override
    public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress)
            throws IOException {
        return prepareDownload(file -> CurseForgeDownload.fetch(uri, file, progress));
    }

    @FunctionalInterface
    interface Fetch {
        void write(Path temporary) throws IOException;
    }

    DownloadQueue.Prepared prepareDownload(Fetch fetch) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".tc4-download-", ".part");
        try {
            fetch.write(temporary);
            var snapshot = target.read(temporary);
            return new DownloadQueue.Prepared() {
                public String apply() throws IOException {
                    return prepare(snapshot);
                }

                public void close() throws IOException {
                    Files.deleteIfExists(temporary);
                }
            };
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
    }

    private String prepare(OriginalResources.Snapshot snapshot) throws IOException {
        if (!Files.isDirectory(snapshot.file()))
            ProcessedOriginalResources.store(game, target, snapshot);
        if (snapshot.sha256().equals(target.sha256())) return "";
        String warning = target.name() + " · SHA-256 differs from reference: " + snapshot.sha256();
        ResourceLoadingState.warning(warning);
        return warning;
    }
}
