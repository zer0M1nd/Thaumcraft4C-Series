package tc4revival.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;

import java.nio.file.Path;
import java.io.IOException;
import java.net.URI;
import tc4revival.download.DownloadQueue;
import java.util.*;
import java.util.function.Consumer;

/** Collect translation requests for the startup screen and expose locally available packs. */
public class LegacyLocalizationLoading {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static Optional<I18nUpdateBridge> bridge = Optional.empty();

    private LegacyLocalizationLoading() {}

    static void collect(Path game, List<DownloadQueue.Task> missing, boolean chinese)
            throws Exception {
        // Player-supplied, verified caches remain available even when downloads are disabled.
        if (!chinese) {
            try { bridge = I18nUpdateBridge.find(); }
            catch (ReflectiveOperationException | LinkageError e) { LOGGER.debug("Cached I18n integration unavailable", e); }
            return;
        }
        collectOptional("official-supplements", "中文补充包", game.resolve("resources/thaumcraft-localization"),
                missing, tasks -> {
                    var seen = new HashSet<String>();
                    for (var source : LegacyOfficialTranslations.installed())
                        if (seen.add(source.sha256())) OriginalDownloadBootstrap.addIfMissing(tasks, TranslationDownload.official(game, source));
                });
        collectOptional("i18n-catalog", "I18nUpdateMod · 中文补充包", I18nUpdateBridge.cache(game),
                missing, tasks -> collectArchives(game, tasks));
    }

    @FunctionalInterface
    private interface Catalog { void collect(List<DownloadQueue.Task> tasks) throws Exception; }

    private static void collectOptional(String id, String name, Path directory, List<DownloadQueue.Task> missing, Catalog catalog) {
        var initial = new ArrayList<DownloadQueue.Task>();
        try {
            catalog.collect(initial);
            missing.addAll(initial);
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Cannot check optional translation catalog " + id, e);
            missing.add(new DownloadQueue.Task() {
                private List<DownloadQueue.Task> archives = List.of();
                public DownloadQueue.Item item() {
                    return new DownloadQueue.Item(id, name, "MC 1.7.10", directory.toString(), DownloadQueue.Kind.OPTIONAL, List.of());
                }
                public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress) throws IOException { return check(); }
                public DownloadQueue.Prepared check() throws IOException {
                    var tasks = new ArrayList<DownloadQueue.Task>();
                    try { catalog.collect(tasks); }
                    catch (Exception | LinkageError failure) { throw new IOException(name + ": " + failure, failure); }
                    return () -> { archives = List.copyOf(tasks); return ""; };
                }
                public List<DownloadQueue.Task> followUp() { return archives; }
            });
        }
    }

    private static void collectArchives(Path game, List<DownloadQueue.Task> missing) throws Exception {
        bridge = I18nUpdateBridge.find();
        if (bridge.isPresent()) for (var asset : bridge.get().assets())
            OriginalDownloadBootstrap.addIfMissing(missing, TranslationDownload.archive(game, asset));
    }

    public static void discover(Path game, Consumer<Pack> consumer) {
        try {
            var official =
                    LegacyLocalization.prepare(
                            game,
                            LegacyOfficialTranslations.load(
                                    game, LegacyOfficialTranslations.installed(), false));
            add(
                    consumer,
                    LegacyOfficialTranslations.PACK_ID,
                    "Legacy maintainer Chinese supplements",
                    official);
            if (bridge.isPresent()) {
                var prepared =
                        LegacyLocalization.prepare(
                                game,
                                I18nUpdateBridge.cached(game),
                                LegacyLocalization.installed());
                add(
                        consumer,
                        LegacyLocalization.PACK_ID,
                        "Thaumcraft 1.7.10 Chinese fallback (CFPA)",
                        prepared);
            }
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Cannot prepare cached legacy translations", e);
        }
    }

    private static void add(
            Consumer<Pack> consumer,
            String packId,
            String name,
            LegacyLocalization.Prepared current) {
        if (current == null || current.entries() == 0) return;
        Pack.ResourcesSupplier supplier =
                id -> new ExternalResourcePack(id, current.directory(), current.allowed());
        var info = Pack.readPackInfo(packId, supplier);
        if (info != null)
            consumer.accept(
                    Pack.create(
                            packId,
                            Component.literal(name),
                            true,
                            supplier,
                            info,
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.BOTTOM,
                            true,
                            PackSource.BUILT_IN));
    }
}
