package tc4revival.resources;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/** A repository source is queried again on Retry. Missing originals never block vanilla GUI startup. */
@Mod.EventBusSubscriber(modid="thaumcraft",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ExternalResourceLoading {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    public static final String PACK_ID="thaumcraft_original_resources";
    @SubscribeEvent public static void addPacks(AddPackFindersEvent event) {
        if(event.getPackType()==PackType.CLIENT_RESOURCES)
            event.addRepositorySource(consumer->discover(FMLPaths.GAMEDIR.get().toAbsolutePath().normalize(),consumer));
    }
    private static void discover(Path game,Consumer<Pack> consumer) {
        LegacyLocalizationLoading.discover(game,consumer);
        var logos = new HashMap<String,byte[]>();
        try {
            var mapping=OriginalResources.mappings();
            var scan=OriginalResourceSelection.scan(game,mapping);
            var source=scan.automatic();
            if(source==null)ResourceLoadingState.failure("Thaumcraft 4.2.3.5: required resources are missing or damaged.\nhttps://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552");
            else {
                if(!source.reference())ResourceLoadingState.warning("Thaumcraft 4: SHA-256 differs from reference: "+source.sha256());
                var cache=ExternalResourceCache.prepare(game,source,mapping);
                var allowed=new HashSet<>(mapping.keySet());allowed.addAll(DeepslateOreTextures.paths());
                mapping.keySet().stream().filter(p->p.endsWith(".png")).forEach(p->allowed.add(p+".mcmeta"));
                allowed.addAll(List.of(ExternalResourceCache.RESEARCH_BUTTON,ExternalResourceCache.CRIMSON_BANNER,ExternalResourceCache.CRIMSON_SHIELD,"pack.mcmeta","assets/thaumcraft/lang/en_us.json","assets/thaumcraft/lang/zh_cn.json","assets/thaumcraft/lang/zh_tw.json"));
                add(consumer,PACK_ID,"Thaumcraft 4 original resources",cache,allowed);
                LOGGER.info("Loaded original TC4 assets from {} (SHA-256 {})",source.file(),source.sha256());
            }
            var requirements = AddonResourceRequirements.installed();
            var primary = new HashMap<String,AddonResourceRequirements.Requirement>();
            var logoOwners = LegacyModLogo.primarySourceOwners();
            for (var requirement : requirements) {
                if (logoOwners.contains(requirement.owner())) primary.putIfAbsent(requirement.owner(), requirement);
            }
            // A participating addon can reserve its modern logoFile even when its old mod has no image.
            primary.keySet().forEach(owner -> logos.put(owner, null));
            var addons=AddonResourceRequirements.scan(game,requirements);
            addons.missing().forEach(ResourceLoadingState::failure);
            addons.warnings().forEach(ResourceLoadingState::warning);
            for(var resolved:addons.resolved()) {
                var owner = resolved.requirement().owner();
                if (primary.get(owner) == resolved.requirement())
                    logos.put(owner, resolved.source().resources().get(LegacyModLogo.CACHE_FILE));
                try {
                    var prepared=AddonResourceRequirements.prepare(game,resolved,resources->{
                        thaumcraft.api.resources.ExternalResourceConverters.prepare(resolved.requirement().owner(),resources);
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new thaumcraft.api.resources.PrepareExternalResourcesEvent(resolved.requirement().owner(),resources));
                    });
                    add(consumer,PACK_ID+"/"+prepared.packId(),prepared.owner()+" original resources",prepared.directory(),prepared.allowed());
                }catch(IOException|RuntimeException failure){ResourceLoadingState.failure(resolved.requirement().name()+": "+failure.getMessage());LOGGER.warn("Cannot prepare addon resources",failure);}
            }
        }catch(IOException|RuntimeException failure){ResourceLoadingState.failure(String.valueOf(failure.getMessage()));LOGGER.warn("Cannot prepare original resource packs",failure);}
        ForgeModLogos.update(game, logos);
    }
    private static void add(Consumer<Pack> consumer,String id,String name,Path directory,Set<String> allowed) throws IOException {
        Pack.ResourcesSupplier supplier=key->new ExternalResourcePack(key,directory,allowed);
        var metadata=Pack.readPackInfo(id,supplier);if(metadata==null)throw new IOException("Cannot read resource pack: "+name);
        consumer.accept(Pack.create(id,Component.literal(name),true,supplier,metadata,PackType.CLIENT_RESOURCES,Pack.Position.BOTTOM,true,PackSource.BUILT_IN));
    }
    private ExternalResourceLoading() {}
}
