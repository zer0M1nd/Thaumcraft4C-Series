package tc4revival.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/** Collect translation requests for the startup screen and expose locally available packs. */
public class LegacyLocalizationLoading {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static Optional<I18nUpdateBridge> bridge = Optional.empty();

    private LegacyLocalizationLoading() {}

    static void collect(Path game, List<tc4revival.download.DownloadQueue.Task> missing)
            throws Exception {
        var seen = new HashSet<String>();
        for (var source : LegacyOfficialTranslations.installed())
            if (seen.add(source.sha256()))
                OriginalDownloadBootstrap.addIfMissing(
                        missing, TranslationDownload.official(game, source));
        try {
            bridge = I18nUpdateBridge.find();
            if (bridge.isPresent())
                for (var asset : bridge.get().assets())
                    OriginalDownloadBootstrap.addIfMissing(
                            missing, TranslationDownload.archive(game, asset));
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Cannot check optional I18n catalog, using cached translations", e);
        }
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
