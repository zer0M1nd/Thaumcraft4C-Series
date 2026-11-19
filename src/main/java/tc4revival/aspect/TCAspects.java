package tc4revival.aspect;

import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public  class TCAspects {
    private static final DeferredRegister<Aspect> ENTRIES = DeferredRegister.create(
            ResourceLocation.fromNamespaceAndPath(TC4Revival.MOD_ID, "aspects"), TC4Revival.MOD_ID);
    public static final Supplier<IForgeRegistry<Aspect>> REGISTRY = ENTRIES.makeRegistry(RegistryBuilder::new);
    private static final java.util.Map<String, RegistryObject<Aspect>> BUILT_INS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, DeferredRegister<Aspect>> ADDONS = new java.util.LinkedHashMap<>();
    private record CatalogCache(IForgeRegistry<Aspect> registry,int size,AspectCatalog catalog) {}
    private static CatalogCache cached;
    @net.minecraftforge.eventbus.api.SubscribeEvent public static synchronized void remapped(net.minecraftforge.registries.IdMappingEvent event) { cached=null; }

    static {
        for (Aspect definition : AspectDefinitions.bundled().all()) {
            var id=ResourceLocation.parse(definition.tag().contains(":")?definition.tag():"thaumcraft:"+definition.tag());
            var register=id.getNamespace().equals("thaumcraft")?ENTRIES:ADDONS.computeIfAbsent(id.getNamespace(),
                    namespace->DeferredRegister.create(ResourceLocation.parse("thaumcraft:aspects"),namespace));
            BUILT_INS.put(definition.tag(), register.register(id.getPath(), () -> definition));
        }
    }

    private TCAspects() {}
    public static void register(IEventBus bus) { ENTRIES.register(bus); ADDONS.values().forEach(entries->entries.register(bus)); }
    public static Aspect get(LegacyAspect definition) { return BUILT_INS.get(definition.tag()).get(); }

    /** Runtime snapshot in legacy order, resolved only after registration. */
    public static synchronized AspectCatalog catalog() {
        var registry=REGISTRY.get();int size=registry.getKeys().size();
        if(cached!=null&&cached.registry()==registry&&cached.size()==size)return cached.catalog();
        var ordered=new java.util.LinkedHashMap<String,Aspect>();
        BUILT_INS.forEach((tag,entry)->ordered.put(tag,entry.get()));
        registry.getValues().forEach(value->ordered.putIfAbsent(value.tag(),value));
        var result=new AspectCatalog(ordered.values(),thaumcraft.api.data.StartupDefinitions.rules().allowAspectCycles(),thaumcraft.api.data.StartupDefinitions.rules().aspectAliases());cached=new CatalogCache(registry,size,result);return result;
    }
}
