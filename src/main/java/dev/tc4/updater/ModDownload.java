package dev.tc4.updater;

import dev.tc4.download.DownloadQueue;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

/** Stages a verified network response or the actual bytes of a player-supplied release. */
final class ModDownload implements DownloadQueue.Task {
    private final ReleaseApi api;
    private final ReleaseApi.Release release;
    private final Path mods, directory;
    private final DownloadQueue.Item item;
    private final InstalledFiles files;
    private final Consumer<String> log;
    private Path prepared;
    private Path manual;
    private String hash;

    ModDownload(
            ReleaseApi api,
            ReleaseApi.Release release,
            String name,
            String current,
            Path directory,
            InstalledFiles files,
            Consumer<String> log) {
        this.api = api;
        this.release = release;
        this.mods = files.mods;
        this.directory = directory;
        this.files = files;
        this.log = log;
        String detail = current == null ? release.version() : current + " → " + release.version();
        this.item =
                new DownloadQueue.Item(
                        release.modid(),
                        name,
                        detail,
                        mods.toString(),
                        current == null ? DownloadQueue.Kind.INSTALL : DownloadQueue.Kind.UPDATE,
                        api.links(release),
                        release.url()
                                .resolve("/docs/changelogs/" + release.modid() + ".html")
                                .toString());
    }

    @Override
    public DownloadQueue.Item item() {
        return item;
    }

    Path staged() {
        return prepared;
    }

    Path manual() {
        return manual;
    }

    String hash() {
        return hash;
    }

    @Override
    public DownloadQueue.Prepared local() throws IOException {
        for (var named : files.matching(release.modid(), release.version())) {
            Path copy = Files.createTempFile(directory, "manual-", ".part");
            try {
                Files.copy(named, copy, StandardCopyOption.REPLACE_EXISTING);
                UpdateEngine.requireCandidate(ModMetadata.read(copy), release);
                return result(copy, named, FilesSupport.sha256(copy));
            } catch (IOException ex) {
                Files.deleteIfExists(copy);
            }
        }
        return null;
    }

    @Override
    public DownloadQueue.Prepared download(URI source, DownloadQueue.Progress progress)
            throws IOException {
        Path copy = directory.resolve(UUID.randomUUID() + ".part");
        try {
            api.download(release, source, copy, progress);
            UpdateEngine.requireCandidate(ModMetadata.read(copy), release);
            return result(copy, null, release.sha256());
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(copy);
            throw ex;
        }
    }

    private DownloadQueue.Prepared result(Path copy, Path original, String actual) {
        return new DownloadQueue.Prepared() {
            public String apply() throws IOException {
                Path target = directory.resolve(release.modid() + ".ready");
                Files.move(copy, target, StandardCopyOption.REPLACE_EXISTING);
                prepared = target;
                manual = original;
                hash = actual;
                if (actual.equals(release.sha256())) return "";
                String warning =
                        release.modid() + " " + release.version() + " · SHA-256 differs: " + actual;
                log.accept(warning);
                return warning;
            }

            public void close() throws IOException {
                Files.deleteIfExists(copy);
            }
        };
    }

    static final class InstalledFiles {
        private record Cached(BasicFileAttributes stamp, ModMetadata metadata) {}

        private final Path mods;
        private final Map<Path, Cached> cache = new HashMap<>();

        InstalledFiles(Path mods) {
            this.mods = mods;
        }

        ModMetadata scan(Path path, Set<String> managed) throws IOException {
            var stamp =
                    Files.readAttributes(
                            path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            var metadata = ModMetadata.scan(path, managed);
            cache.put(path, new Cached(stamp, metadata));
            return metadata;
        }

        List<Path> matching(String id, String version) throws IOException {
            var matches = new ArrayList<Path>();
            try (var stream = Files.list(mods)) {
                for (Path path :
                        stream.filter(
                                        p ->
                                                p.getFileName()
                                                        .toString()
                                                        .toLowerCase(Locale.ROOT)
                                                        .endsWith(".jar"))
                                .sorted()
                                .toList()) {
                    try {
                        var stamp =
                                Files.readAttributes(
                                        path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                        if (!stamp.isRegularFile()) continue;
                        var prior = cache.get(path);
                        if (prior == null
                                || prior.stamp.size() != stamp.size()
                                || !prior.stamp.lastModifiedTime().equals(stamp.lastModifiedTime())
                                || !Objects.equals(prior.stamp.fileKey(), stamp.fileKey())) {
                            prior = new Cached(stamp, ModMetadata.scan(path, Set.of()));
                            cache.put(path, prior);
                        }
                        if (version.equals(prior.metadata.mods().get(id))) matches.add(path);
                    } catch (IOException ignored) {
                        /* A file may still be moving into the directory. */
                    }
                }
            }
            return matches;
        }
    }
}
