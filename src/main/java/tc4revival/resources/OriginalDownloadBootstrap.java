package tc4revival.resources;

import net.minecraftforge.fml.loading.FMLPaths;

import tc4revival.download.DownloadQueue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Local preparation before pack discovery. The screen owns the start of every network transfer. */
public final class OriginalDownloadBootstrap {
    private static List<DownloadQueue.Task> pending;
    private static String discoveryError;
    private static final java.util.concurrent.atomic.AtomicBoolean attempted =
            new java.util.concurrent.atomic.AtomicBoolean();

    public static synchronized List<DownloadQueue.Task> pending() {
        // Some startup paths register pack finders before the Forge hook. Recheck once the mod list is available.
        if (pending == null) discover();
        return pending;
    }

    public static synchronized String discoveryError() {
        return discoveryError;
    }

    public static void beforeResourceFinders() {
        if (!attempted.compareAndSet(false, true)) return;
        var scan =
                java.util.concurrent.CompletableFuture.runAsync(
                        OriginalDownloadBootstrap::discover);
        while (!scan.isDone()) {
            try {
                scan.get(50, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException waiting) {
                org.lwjgl.glfw.GLFW.glfwPollEvents();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during local resource discovery", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new IllegalStateException("Cannot discover resources", e.getCause());
            }
        }
        scan.join();
    }

    public static synchronized void discover() {
        Path game = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        var missing = new ArrayList<DownloadQueue.Task>();
        discoveryError = null;
        try {
            boolean chinese = tc4revival.download.DownloadPreferences.chinese(game);
            var targets = OriginalDownloadTargets.installed();
            var owners = LegacyModLogo.primarySourceOwners();
            var logos = new HashSet<String>();
            for (var target : targets)
                if (owners.contains(target.owner()))
                    logos.add(
                            ProcessedOriginalResources.directoryName(
                                    target.legacyModId(), target.minecraftVersion()));
            for (var target : OriginalResourceDownloads.grouped(targets))
                addIfMissing(
                        missing,
                        new OriginalArchiveTask(
                                game,
                                target,
                                logos.contains(
                                        ProcessedOriginalResources.directoryName(
                                                target.legacyModId(), target.minecraftVersion())), chinese));
            LegacyLocalizationLoading.collect(game, missing, chinese);
        } catch (Exception e) {
            discoveryError = Objects.toString(e.getMessage(), e.toString());
            ResourceLoadingState.failure(discoveryError);
        }
        pending = List.copyOf(missing);
    }

    static void addIfMissing(List<DownloadQueue.Task> missing, DownloadQueue.Task task)
            throws IOException {
        try (var local = task.local()) {
            if (local == null) missing.add(task);
            else local.apply();
        }
    }
}
