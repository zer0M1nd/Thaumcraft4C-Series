package tc4revival.resources;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Download off the render thread; apply one reload once the initial loading overlay has closed. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class LegacyLocalizationLoading {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static volatile boolean reloadPending;
    private static volatile LegacyLocalization.Prepared prepared;
    private static volatile LegacyLocalization.Prepared official;
    private static boolean discovered;
    private static Optional<I18nUpdateBridge> bridge = Optional.empty();
    private LegacyLocalizationLoading() {}

    public static synchronized void discover(Path game, Consumer<Pack> consumer) {
        try {
            if (!discovered) { discovered=true; bridge=I18nUpdateBridge.find(); }
            var mappings = LegacyLocalization.installed();
            var supplements=LegacyOfficialTranslations.installed();
            if(official==null&&!supplements.isEmpty())official=LegacyLocalization.prepare(game,LegacyOfficialTranslations.load(game,supplements,false));
            if (bridge.isPresent() && prepared == null) try {
                var cached = I18nUpdateBridge.cached(game);
                if (!cached.isEmpty()) prepared=LegacyLocalization.prepare(game,cached,mappings);
            } catch (Exception damaged) { LOGGER.warn("Cannot use cached legacy translations; attempting repair",damaged); }
            if ((!supplements.isEmpty()||bridge.isPresent())&&STARTED.compareAndSet(false,true)) {
                Thread worker = new Thread(() -> {
                    if(!supplements.isEmpty())try {
                        var next=LegacyLocalization.prepare(game,LegacyOfficialTranslations.load(game,supplements,true));
                        var previous=official;official=next;
                        if(next.entries()>0&&(previous==null||!previous.directory().equals(next.directory())))reloadPending=true;
                        LOGGER.info("Legacy maintainer Chinese supplements ready: {} entries",next.entries());
                    }catch(Exception failure){LOGGER.warn("Legacy maintainer Chinese supplements unavailable",failure);}
                    if(bridge.isEmpty())return;
                    try {
                        var next = LegacyLocalization.prepare(game,bridge.get().update(game),mappings);
                        var previous = prepared;
                        prepared=next;
                        if(next.entries()>0 && (previous==null || !previous.directory().equals(next.directory())))reloadPending=true;
                        LOGGER.info("I18nUpdateMod 1.7.10 fallback ready: {} mapped Chinese entries; raw downloads remain unselected in {}",next.entries(),I18nUpdateBridge.cache(game));
                    } catch (Exception | LinkageError failure) {
                        LOGGER.warn("Optional I18nUpdateMod 1.7.10 download unavailable; keeping existing translations",failure);
                    }
                },"Thaumcraft legacy translations");
                worker.setDaemon(true); worker.start();
            }
            add(consumer,LegacyOfficialTranslations.PACK_ID,"Legacy maintainer Chinese supplements",official);
            if(bridge.isPresent())add(consumer,LegacyLocalization.PACK_ID,"Thaumcraft 1.7.10 Chinese fallback (CFPA)",prepared);
        } catch (Exception | LinkageError failure) { LOGGER.warn("Optional legacy localization unavailable",failure); }
    }
    private static void add(Consumer<Pack> consumer,String packId,String name,LegacyLocalization.Prepared current) {
            if(current==null||current.entries()==0)return;
            Pack.ResourcesSupplier supplier = id -> new ExternalResourcePack(id,current.directory(),current.allowed());
            var info=Pack.readPackInfo(packId,supplier);
            if (info!=null) consumer.accept(Pack.create(packId,
                    Component.literal(name),true,supplier,info,
                    PackType.CLIENT_RESOURCES,Pack.Position.BOTTOM,true,PackSource.BUILT_IN));
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !reloadPending) return;
        var minecraft=Minecraft.getInstance();
        if (minecraft.getOverlay()!=null) return;
        reloadPending=false;
        minecraft.getResourcePackRepository().reload();
        minecraft.reloadResourcePacks();
    }
}
