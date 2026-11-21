package dev.tc4.updater.bootstrap;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import dev.tc4.updater.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;

public final class UpdaterService implements ITransformationService {
    private static final System.Logger LOG = System.getLogger("ThaumcraftUpdater");
    static Path gameDirectory;
    private static UpdateEngine.Session session;
    @Override public String name() { return "thaumcraftupdater"; }
    @Override public void onLoad(IEnvironment environment, Set<String> otherServices) {}
    @Override public void initialize(IEnvironment environment) {
        gameDirectory = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElseThrow().toAbsolutePath().normalize();
        String target = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        if (!target.equals("forgeclient") && !target.equals("forgeserver")) {
            log("Skipping updates for development/non-production launch target: " + target);
            return;
        }
        // Do not depend on whether FML's initialize() runs before or after this service.
        String loaderVersion = FMLLoader.class.getModule().getDescriptor().rawVersion().orElse(null);
        if (loaderVersion == null || !loaderVersion.startsWith("1.20.1-47."))
            throw new IllegalStateException("ThaumcraftUpdater requires Minecraft 1.20.1 / Forge 47");
        var context = new UpdateEngine.Context("1.20.1", loaderVersion.substring("1.20.1-".length()),
                target.equals("forgeclient") ? "CLIENT" : "SERVER");
        log("EARLY_UPDATE_BEGIN");
        session = new UpdateEngine(new ReleaseApi(), UpdaterService::log,
                target.equals("forgeclient")?new StartupWindow(gameDirectory,environment,context.forge()):null).start(gameDirectory, context);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { session.close(); }
            catch (java.io.IOException error) { log("Could not release updater lock: " + error.getMessage()); }
        }, "ThaumcraftUpdater lock release"));
        log("EARLY_UPDATE_COMPLETE: " + session.result().message());
        SelfUpdate.launch(gameDirectory, UpdaterService::log);
    }
    private static void log(String message) { LOG.log(System.Logger.Level.INFO, "[ThaumcraftUpdater] " + message); }
    @Override @SuppressWarnings("rawtypes") public List<ITransformer> transformers() { return List.of(); }
}
