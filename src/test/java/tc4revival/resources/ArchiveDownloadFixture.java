package tc4revival.resources;

import tc4revival.download.DownloadQueue;
import tc4revival.resources.OriginalResourceDownloads.Result;
import tc4revival.resources.OriginalResourceDownloads.Target;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/** Exercises the production archive task with a fixture transport instead of a rendering client. */
final class ArchiveDownloadFixture {
    @FunctionalInterface
    interface Transfer {
        void fetch(URI source, Path file) throws IOException;
    }

    static List<Result> obtainMissing(
            Path game, List<Target> targets, Consumer<String> progress, Transfer transfer) {
        return obtainMissing(game, targets, Set.of(), progress, transfer);
    }

    static List<Result> obtainMissing(
            Path game,
            List<Target> targets,
            Set<String> logos,
            Consumer<String> progress,
            Transfer transfer) {
        var results = new ArrayList<Result>();
        for (var target : OriginalResourceDownloads.grouped(targets))
            try {
                String id =
                        ProcessedOriginalResources.directoryName(
                                target.legacyModId(), target.minecraftVersion());
                var task = new OriginalArchiveTask(game, target, logos.contains(id));
                Path cache = game.resolve("thaumcraft").resolve(id);
                try (var existing = task.local()) {
                    if (existing != null) {
                        String warning = existing.apply();
                        if (!warning.isEmpty())
                            results.add(new Result(target.owner(), cache, warning, ""));
                        continue;
                    }
                }
                IOException last = null;
                for (URI uri : task.item().links()) {
                    progress.accept(uri.toString());
                    try (var downloaded = task.prepareDownload(uri, file -> transfer.fetch(uri, file))) {
                        results.add(new Result(target.owner(), cache, downloaded.apply(), ""));
                        last = null;
                        break;
                    } catch (IOException failure) {
                        last = failure;
                    }
                }
                if (last != null) {
                    if (task.item().kind() == DownloadQueue.Kind.LOGO)
                        results.add(new Result(target.owner(), cache, last.toString(), ""));
                    else throw last;
                }
            } catch (IOException failure) {
                results.add(new Result(target.owner(), null, "", failure.toString()));
            }
        return List.copyOf(results);
    }
}
