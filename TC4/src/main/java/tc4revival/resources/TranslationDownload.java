package tc4revival.resources;

import tc4revival.download.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Downloaded, previously cached and dropped translations must match the publisher's digest. */
final class TranslationDownload implements DownloadQueue.Task {
    @FunctionalInterface interface Check { String digest(Path path) throws IOException; }
    @FunctionalInterface interface Installed { void accept(Path path) throws IOException; }
    private final Path file;
    private final DownloadQueue.Item item;
    private final long limit;
    private final Map<String,String> hashes;
    private final Check verify;
    private final Installed installed;
    private final Predicate<URI> allowed;

    private TranslationDownload(Path file, String label, String version, List<URI> links, List<URI> pages,
            long limit, Map<String,String> hashes, Check verify, Installed installed, Predicate<URI> allowed) {
        this.file = file;
        item = new DownloadQueue.Item(file.toString(), label, version, file.toString(), DownloadQueue.Kind.OPTIONAL,
                links, ManualLink.ordered(pages, true), null);
        this.limit = limit;
        this.hashes = hashes;
        this.verify = verify;
        this.installed = installed;
        this.allowed = allowed;
    }

    static TranslationDownload official(Path game, LegacyOfficialTranslations.Source source) {
        Path file = game.resolve("resources/thaumcraft-localization/official").resolve(source.sha256() + ".lang");
        String[] path = source.url().getRawPath().substring(1).split("/", 4);
        return new TranslationDownload(file, source.owner() + " · 中文补全", path[2].substring(0, 8),
                source.downloads(), source.sources().manualPages().stream().map(URI::create).toList(),
                8L * 1024 * 1024, source.sources().hashes(), p -> {
                    byte[] bytes = Files.readAllBytes(p);
                    if (LegacyLocalization.parse(bytes).isEmpty()) throw new IOException("Missing language entries");
                    return OriginalResources.sha256(bytes);
                }, p -> {}, source.downloads()::contains);
    }

    static TranslationDownload archive(Path game, I18nUpdateBridge.Asset asset) {
        Path file = I18nUpdateBridge.cache(game).resolve(asset.name());
        return new TranslationDownload(file, asset.name(), asset.version(), List.of(asset.url()), List.of(asset.url()),
                128L * 1024 * 1024, Map.of(asset.url().toString(), asset.md5()), p -> {
                    I18nUpdateBridge.validateZip(p);
                    return I18nUpdateBridge.md5(p);
                }, p -> I18nUpdateBridge.record(game, asset, p), CurseForgeDownload::webLink);
    }

    public DownloadQueue.Item item() { return item; }

    public DownloadQueue.Prepared local() throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > limit) return null;
        try { if (!hashes.containsValue(verify.digest(file))) return null; }
        catch (IOException | RuntimeException ex) { return null; }
        return () -> { installed.accept(file); return ""; };
    }

    public DownloadQueue.Prepared dropped(DownloadQueue.DroppedFile input) throws IOException {
        if (input.size() > limit) return null;
        String hash = hashes.containsValue(input.sha256()) ? input.sha256() : input.md5();
        if (!hashes.containsValue(hash)) return null;
        return stage(hash, temp -> Files.copy(input.path(), temp, StandardCopyOption.REPLACE_EXISTING));
    }

    public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress) throws IOException {
        return prepareDownload(uri, temp -> CurseForgeDownload.transfer(uri, temp, allowed, limit, 7000, 60000, progress));
    }

    DownloadQueue.Prepared prepareDownload(URI uri, OriginalArchiveTask.Fetch fetch) throws IOException {
        String hash = hashes.get(uri.toString());
        if (hash == null || !item.links().contains(uri)) throw new IOException("No checksum for this download source");
        return stage(hash, fetch);
    }

    private DownloadQueue.Prepared stage(String expected, OriginalArchiveTask.Fetch fetch) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), ".language-", ".part");
        try {
            fetch.write(temp);
            if (Files.size(temp) > limit) throw new IOException("Translation file exceeds size limit");
            if (!expected.equals(verify.digest(temp))) throw new IOException("Checksum mismatch for " + item.name());
            return new DownloadQueue.Prepared() {
                public String apply() throws IOException {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                    installed.accept(file);
                    return "";
                }
                public void close() throws IOException { Files.deleteIfExists(temp); }
            };
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }
    }
}
