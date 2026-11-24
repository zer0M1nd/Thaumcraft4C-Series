package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import tc4revival.download.DownloadQueue;
import tc4revival.download.ui.DownloadScreen;
import tc4revival.resources.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "thaumcraft", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class OriginalResourceScreen {
    private static boolean completed, warned;
    private static Screen active;

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
        Screen gate = gate(continuation);
        if (gate == null) return false;
        Minecraft.getInstance().setScreen(gate);
        return true;
    }

    // Forge's loading-warning screen opens the title screen directly, bypassing setInitialScreen.
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void opening(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        if (next instanceof net.minecraft.client.gui.screens.TitleScreen
                || next instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            Screen gate = gate(() -> Minecraft.getInstance().setScreen(next));
            if (gate != null) event.setNewScreen(gate);
        }
    }

    private static Screen gate(Runnable continuation) {
        if (completed) return null;
        if (active != null) return active;
        var tasks = OriginalDownloadBootstrap.pending();
        String error = OriginalDownloadBootstrap.discoveryError();
        if (tasks.isEmpty() && error == null && !ResourceLoadingState.missing()) {
            completed = true;
            showWarnings();
            return null;
        }
        if (tasks.isEmpty() || error != null) {
            tasks = new ArrayList<>(tasks);
            tasks.add(repairTask(error != null ? error : ResourceLoadingState.details()));
        }
        return screen(() -> {
            completed = true;
            active = null;
            continuation.run();
        }, tasks, false);
    }

    private static DownloadQueue.Task repairTask(String problem) {
        return new DownloadQueue.Task() {
            private boolean reported;
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

            private List<DownloadQueue.Task> followUp = List.of();
            public List<DownloadQueue.Task> followUp() { return followUp; }
            public DownloadQueue.Prepared check() throws IOException {
                if (!reported) {
                    reported = true;
                    throw new IOException(problem);
                }
                OriginalDownloadBootstrap.discover();
                if (OriginalDownloadBootstrap.discoveryError() != null)
                    throw new IOException(OriginalDownloadBootstrap.discoveryError());
                return () -> { followUp = OriginalDownloadBootstrap.pending(); return ""; };
            }

            public DownloadQueue.Prepared download(URI uri, DownloadQueue.Progress progress) {
                return null;
            }
        };
    }

    private static Screen screen(Runnable continuation, List<DownloadQueue.Task> tasks, boolean previouslyAcceptedMissing) {
        var queue = new DownloadQueue(tasks);
        var mc = Minecraft.getInstance();
        active = new DownloadScreen(
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
                        var followUp = tasks.stream().filter(t -> !queue.skipped(t.item().id()))
                                .flatMap(t -> t.followUp().stream()).toList();
                        boolean acceptedMissing = previouslyAcceptedMissing || tasks.stream().anyMatch(t -> t.item().kind() == DownloadQueue.Kind.RESOURCE && queue.skipped(t.item().id()));
                        if (!followUp.isEmpty()) {
                            mc.setScreen(screen(continuation, followUp, acceptedMissing));
                            return true;
                        }
                        showWarnings();
                        reload(continuation, acceptedMissing);
                        return true;
                    }
                });
        return active;
    }

    private static void reload(Runnable continuation, boolean acceptedMissing) {
        var mc = Minecraft.getInstance();
        ResourceLoadingState.clear();
        mc.reloadResourcePacks().whenComplete((ok, error) -> mc.execute(() -> {
            if (error != null) ResourceLoadingState.failure(Objects.toString(error.getMessage(), error.toString()));
            if (ResourceLoadingState.missing() && !acceptedMissing) {
                var repair = repairTask(ResourceLoadingState.details());
                mc.setScreen(screen(continuation, List.of(repair), false));
            } else continuation.run();
        }));
    }
}
