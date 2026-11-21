package tc4revival.resources;

import tc4revival.download.DownloadQueue;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Optional data-only sources use the same visible transfer, cancellation and local-file checks as
 * original JARs.
 */
final class TranslationDownload implements DownloadQueue.Task {
    @FunctionalInterface
    interface Check {
        String check(Path path) throws IOException;
    }

    @FunctionalInterface
    interface Installed {
        void accept(Path path) throws IOException;
    }

    private final Path file;
    private final DownloadQueue.Item item;
    private final long limit;
    private final Check verify;
    private final Installed installed;
    private final Predicate<URI> allowed;

    private TranslationDownload(
            Path file,
            String label,
            String version,
            List<URI> links,
            long limit,
            Check verify,
            Installed installed,
            Predicate<URI> allowed) {
        this.file = file;
        this.item =
                new DownloadQueue.Item(
                        file.toString(),
                        label,
                        version,
                        file.toString(),
                        DownloadQueue.Kind.OPTIONAL,
                        links);
        this.limit = limit;
        this.verify = verify;
        this.installed = installed;
        this.allowed = allowed;
    }

    static TranslationDownload official(Path game, LegacyOfficialTranslations.Source source) {
        Path file =
                game.resolve("resources/thaumcraft-localization/official")
                        .resolve(source.sha256() + ".lang");
        String[] path = source.url().getRawPath().substring(1).split("/", 4);
        URI mirror =
                URI.create(
                        "https://cdn.jsdelivr.net/gh/"
                                + path[0]
                                + "/"
                                + path[1]
                                + "@"
                                + path[2]
                                + "/"
                                + path[3]);
        return new TranslationDownload(
                file,
                source.owner() + " · 中文补全",
                path[2].substring(0, 8),
                List.of(source.url(), mirror),
                8L * 1024 * 1024,
                p -> {
                    byte[] bytes = Files.readAllBytes(p);
                    if (LegacyLocalization.parse(bytes).isEmpty())
                        throw new IOException("Missing language entries");
                    return source.sha256().equals(OriginalResources.sha256(bytes))
                            ? ""
                            : "SHA-256 differs from reference";
                },
                p -> {},
                uri -> source.url().equals(uri) || mirror.equals(uri));
    }

    static TranslationDownload archive(Path game, I18nUpdateBridge.Asset asset) {
        Path file = I18nUpdateBridge.cache(game).resolve(asset.name());
        return new TranslationDownload(
                file,
                asset.name(),
                asset.version(),
                List.of(asset.url()),
                128L * 1024 * 1024,
                p -> {
                    I18nUpdateBridge.validateZip(p);
                    if (p.equals(file) && I18nUpdateBridge.stale(game, asset, p))
                        throw new IOException("An updated translation archive is available");
                    return I18nUpdateBridge.md5(p).equals(asset.md5())
                            ? ""
                            : "MD5 differs from reference";
                },
                p -> I18nUpdateBridge.record(game, asset, p),
                CurseForgeDownload::webLink);
    }

    @Override
    public DownloadQueue.Item item() {
        return item;
    }

    @Override
    public DownloadQueue.Prepared local() throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > limit)
            return null;
        String warning;
        try {
            warning = verify.check(file);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
        return () -> {
            installed.accept(file);
            return warn(warning);
        };
    }

    private String warn(String warning) {
        if (!warning.isEmpty()) ResourceLoadingState.warning(item.name() + " · " + warning);
        return warning;
    }

    @Override
    public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress)
            throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), ".language-", ".part");
        try {
            CurseForgeDownload.transfer(uri, temp, allowed, limit, 7000, 60000, progress);
            String warning = verify.check(temp);
            return new DownloadQueue.Prepared() {
                public String apply() throws IOException {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                    installed.accept(file);
                    return warn(warning);
                }

                public void close() throws IOException {
                    Files.deleteIfExists(temp);
                }
            };
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }
    }
}
