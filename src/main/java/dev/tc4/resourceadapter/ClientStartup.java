package dev.tc4.resourceadapter;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.slf4j.Logger;

final class ClientStartup {
    private static final Logger LOG = LogUtils.getLogger();
    private ClientStartup() {}

    static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientStartup::addPacks);
    }

    private static void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path directory = Minecraft.getInstance().getResourcePackDirectory();
        ResourceMapping mapping;
        try { mapping = ResourceMapping.bundled(); }
        catch (IOException error) { throw new IllegalStateException("Cannot load bundled legacy texture mappings", error); }
        var installer = new PackInstaller(LOG::warn);
        var first = new AtomicReference<>(new StartupTask<>(() -> discover(installer, directory, mapping)));
        event.addRepositorySource(consumer -> {
            var initial = first.getAndSet(null);
            var results = initial == null ? discover(installer, directory, mapping) : initial.await(ImmediateWindowHandler::renderTick);
            for (var result : results) {
                var pack = Pack.readMetaAndCreate(result.id(), Component.literal("[TC 1.20.1] " + result.source()), false,
                        id -> new FilePackResources(id, result.pack().toFile(), false), PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP, PackSource.DEFAULT);
                if (pack != null) consumer.accept(pack);
                if (!result.reused()) LOG.info("Adapted '{}' ({} textures) to {}", result.source(), result.textures(), result.pack());
            }
        });
    }

    private static List<PackInstaller.Result> discover(PackInstaller installer, Path directory, ResourceMapping mapping) {
        try { return installer.prepare(directory, mapping); }
        catch (IOException error) {
            LOG.warn("Could not scan local resource pack directory {}", directory, error);
            return List.of();
        }
    }
}
