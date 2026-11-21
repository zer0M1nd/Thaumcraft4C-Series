package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import tc4revival.download.DownloadQueue;
import tc4revival.download.ui.DownloadScreen;
import tc4revival.resources.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public final class OriginalResourceScreen {
    private static boolean opened, warned;

    private static void showWarnings() {
        var warnings = ResourceLoadingState.warnings();
        if (warned || warnings.isEmpty()) return;
        warned = true;
        var mc = Minecraft.getInstance();
        net.minecraft.client.gui.components.toasts.SystemToast.add(
                mc.getToasts(),
                net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds
                        .PACK_LOAD_FAILURE,
                net.minecraft.network.chat.Component.literal("资源校验警告 / Resource warning"),
                net.minecraft.network.chat.Component.literal(warnings.get(0)));
    }

    public static boolean beforeInitialScreen(Runnable continuation) {
        if (opened) return false;
        var tasks = OriginalDownloadBootstrap.pending();
        String error = OriginalDownloadBootstrap.discoveryError();
        if (tasks.isEmpty() && error == null && !ResourceLoadingState.missing()) {
            showWarnings();
            return false;
        }
        opened = true;
        if (tasks.isEmpty()) tasks = List.of(repairTask());
        Minecraft.getInstance().setScreen(screen(continuation, tasks));
        return true;
    }

    private static DownloadQueue.Task repairTask() {
        return new DownloadQueue.Task() {
            public DownloadQueue.Item item() {
                return new DownloadQueue.Item(
                        "resource-check",
                        "资源包检查 / Resource check",
                        ResourceLoadingState.details(),
                        net.minecraftforge.fml.loading.FMLPaths.GAMEDIR
                                .get()
                                .resolve("thaumcraft")
                                .toString(),
                        DownloadQueue.Kind.RESOURCE,
                        List.of());
            }

            public DownloadQueue.Prepared local() throws IOException {
                OriginalDownloadBootstrap.discover();
                if (OriginalDownloadBootstrap.discoveryError() != null)
                    throw new IOException(OriginalDownloadBootstrap.discoveryError());
                return () -> "";
            }

            public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress) {
                return null;
            }
        };
    }

    private static Screen screen(Runnable continuation, List<DownloadQueue.Task> tasks) {
        var queue = new DownloadQueue(tasks);
        var mc = Minecraft.getInstance();
        return new DownloadScreen(
                new DownloadScreen.Controller() {
                    public String snapshot() {
                        return queue.snapshot();
                    }

                    public void action(String action, String id) {
                        queue.action(action, id);
                    }

                    public boolean proceed() {
                        synchronized (queue) {
                            if (!queue.ready()) return false;
                            queue.close();
                        }
                        if (tasks.stream().anyMatch(t -> t.item().id().equals("resource-check"))
                                && !OriginalDownloadBootstrap.pending().isEmpty()) {
                            mc.setScreen(screen(continuation, OriginalDownloadBootstrap.pending()));
                            return true;
                        }
                        showWarnings();
                        ResourceLoadingState.clear();
                        mc.reloadResourcePacks()
                                .whenComplete(
                                        (ok, error) ->
                                                mc.execute(
                                                        () -> {
                                                            if (error != null)
                                                                ResourceLoadingState.failure(
                                                                        Objects.toString(
                                                                                error.getMessage(),
                                                                                error.toString()));
                                                            if (ResourceLoadingState.missing()) {
                                                                OriginalDownloadBootstrap
                                                                        .discover();
                                                                var retry =
                                                                        OriginalDownloadBootstrap
                                                                                .pending();
                                                                mc.setScreen(
                                                                        screen(
                                                                                continuation,
                                                                                retry.isEmpty()
                                                                                        ? List.of(
                                                                                                repairTask())
                                                                                        : retry));
                                                            } else continuation.run();
                                                        }));
                        return true;
                    }
                });
    }
}
