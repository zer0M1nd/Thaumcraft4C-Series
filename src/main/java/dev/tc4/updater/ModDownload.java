package dev.tc4.updater;

import dev.tc4.download.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/** Only verified private copies enter the mods transaction. The dropped original stays in place. */
final class ModDownload implements DownloadQueue.Task {
    private final ReleaseApi api;
    private final ReleaseApi.Release release;
    private final Path directory;
    private final DownloadQueue.Item item;
    private Path prepared, inMods;
    private ReleaseApi.Source selected;

    ModDownload(ReleaseApi api, ReleaseApi.Release release, String name, String current, Path directory, Path mods,
                boolean chinese) {
        this.api = api;
        this.release = release;
        this.directory = directory;
        var kind = release.thirdParty() ? DownloadQueue.Kind.THIRD_PARTY_INSTALL
                : current == null ? DownloadQueue.Kind.INSTALL : DownloadQueue.Kind.UPDATE;
        this.item = new DownloadQueue.Item(release.modid(), name,
                current == null ? release.version() : current + " → " + release.version(), mods.toString(),
                kind, api.links(release),
                ManualLink.ordered(api.manualPages(release), chinese),
                release.thirdParty() ? null : release.url().resolve("/docs/changelogs/" + release.modid() + ".html").toString());
    }

    public DownloadQueue.Item item() { return item; }
    Path staged() { return prepared; }
    Path inMods() { return inMods; }

    ReleaseApi.Release verifiedRelease() {
        return new ReleaseApi.Release(release.modid(), release.version(), release.filename(),
                selected.sha256(), selected.size(), selected.url(), List.of(), List.of(), List.of(), release.thirdParty(), release.additionalMods());
    }

    public DownloadQueue.Prepared dropped(DownloadQueue.DroppedFile file) throws IOException {
        var expected = release.matching(file.sha256(), file.size());
        if (expected == null) return null;
        Path copy = Files.createTempFile(directory, "manual-", ".part");
        try {
            Files.copy(file.path(), copy, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(copy) != expected.size() || !FilesSupport.sha256(copy).equals(expected.sha256()))
                throw new IOException("SHA-256 mismatch after copying " + file.path().getFileName());
            Path original = file.path().toAbsolutePath().normalize();
            Path installed = original.getParent().equals(Path.of(item.directory()).toAbsolutePath().normalize()) ? original : null;
            return result(copy, expected, installed);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(copy);
            throw e;
        }
    }

    public DownloadQueue.Prepared download(URI source, DownloadQueue.Progress progress) throws IOException {
        Path copy = directory.resolve(UUID.randomUUID() + ".part");
        try {
            api.download(release, source, copy, progress);
            return result(copy, release.from(source), null);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(copy);
            throw e;
        }
    }

    private DownloadQueue.Prepared result(Path copy, ReleaseApi.Source expected, Path installed) throws IOException {
        UpdateEngine.requireCandidate(ModMetadata.read(copy), release);
        return new DownloadQueue.Prepared() {
            public String apply() throws IOException {
                Path target = directory.resolve(release.modid() + ".ready");
                Files.move(copy, target, StandardCopyOption.REPLACE_EXISTING);
                prepared = target;
                selected = expected;
                inMods = installed;
                return "";
            }
            public void close() throws IOException { Files.deleteIfExists(copy); }
        };
    }
}
